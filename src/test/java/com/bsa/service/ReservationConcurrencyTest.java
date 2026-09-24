package com.bsa.service;

import com.bsa.model.Book;
import com.bsa.model.BorrowingRecord;
import com.bsa.model.Reservation;
import com.bsa.model.User;
import com.bsa.repository.ReservationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ReservationConcurrencyTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private BookService bookService;

    @Autowired
    private CartService cartService;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ReservationRepository reservationRepository;

    @Test
    void uniqueConstraint_rejectsSecondActiveReservation_evenBypassingServiceChecks() {
        User owner = authService.register("Owner", "owner-uc@example.com", "Pass123");
        User borrower = authService.register("Borrower", "borrower-uc@example.com", "Pass123");
        User waiter = authService.register("Waiter", "waiter-uc@example.com", "Pass123");
        Book book = bookService.addBook(owner.getId(), "Constrained Book", "Some Author", "Fiction", true);
        borrowOut(borrower, book);

        // Go straight through the repository, skipping ReservationService's duplicate check,
        // to prove the DB schema itself (not just Java logic) enforces the invariant.
        reservationRepository.saveAndFlush(new Reservation(book, waiter, LocalDateTime.now()));

        Reservation duplicate = new Reservation(book, waiter, LocalDateTime.now());
        assertThrows(DataIntegrityViolationException.class,
                () -> reservationRepository.saveAndFlush(duplicate));
    }

    @Test
    void concurrentReserveAttempts_bySameUser_onlyOneSucceeds() throws Exception {
        User owner = authService.register("Owner", "owner-race@example.com", "Pass123");
        User borrower = authService.register("Borrower", "borrower-race@example.com", "Pass123");
        User waiter = authService.register("Waiter", "waiter-race@example.com", "Pass123");
        Book book = bookService.addBook(owner.getId(), "Contested Book", "Some Author", "Fiction", true);
        borrowOut(borrower, book);

        CountDownLatch startingGate = new CountDownLatch(2);
        List<RaceResult<Reservation>> results = runConcurrently(2, startingGate,
                () -> reservationService.reserveBook(waiter.getId(), book.getId()));

        long successes = results.stream().filter(RaceResult::succeeded).count();
        long failures = results.stream().filter(r -> !r.succeeded()).count();
        assertEquals(1, successes, "exactly one concurrent reserve attempt should win");
        assertEquals(1, failures, "the other attempt should be rejected, not silently duplicated");
        results.stream().filter(r -> !r.succeeded())
                .forEach(r -> assertInstanceOf(IllegalStateException.class, r.error()));

        assertEquals(1, reservationService.getUserReservations(waiter.getId()).size());
    }

    @Test
    void concurrentClaimAttempts_onSameReservation_onlyOneSucceeds() throws Exception {
        User owner = authService.register("Owner", "owner-claim-race@example.com", "Pass123");
        User borrower = authService.register("Borrower", "borrower-claim-race@example.com", "Pass123");
        User waiter = authService.register("Waiter", "waiter-claim-race@example.com", "Pass123");
        Book book = bookService.addBook(owner.getId(), "Hotly Contested Book", "Some Author", "Fiction", true);
        borrowOut(borrower, book);

        Reservation reservation = reservationService.reserveBook(waiter.getId(), book.getId());
        bookService.returnBook(book.getId(), borrower.getId());

        CountDownLatch startingGate = new CountDownLatch(2);
        List<RaceResult<BorrowingRecord>> results = runConcurrently(2, startingGate,
                () -> reservationService.claimReservation(waiter.getId(), reservation.getId(), "COURIER"));

        long successes = results.stream().filter(RaceResult::succeeded).count();
        long failures = results.stream().filter(r -> !r.succeeded()).count();
        assertEquals(1, successes, "exactly one concurrent claim attempt should win");
        assertEquals(1, failures, "the other attempt must not create a second borrowing record");
        results.stream().filter(r -> !r.succeeded())
                .forEach(r -> assertInstanceOf(IllegalStateException.class, r.error()));
    }

    private <T> List<RaceResult<T>> runConcurrently(int threadCount, CountDownLatch startingGate,
                                                    Supplier<T> action) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            List<Callable<RaceResult<T>>> tasks = List.of(
                    () -> attempt(startingGate, action),
                    () -> attempt(startingGate, action));
            List<Future<RaceResult<T>>> futures = executor.invokeAll(tasks, 10, TimeUnit.SECONDS);
            return futures.stream().map(f -> {
                try {
                    return f.get();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).toList();
        } finally {
            executor.shutdownNow();
        }
    }

    private <T> RaceResult<T> attempt(CountDownLatch startingGate, Supplier<T> action) {
        startingGate.countDown();
        try {
            startingGate.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        try {
            return RaceResult.success(action.get());
        } catch (RuntimeException ex) {
            return RaceResult.failure(ex);
        }
    }

    private void borrowOut(User borrower, Book book) {
        cartService.addToCart(borrower.getId(), book.getId());
        cartService.checkout(borrower.getId(), "COURIER");
    }

    private record RaceResult<T>(T value, RuntimeException error) {
        static <T> RaceResult<T> success(T value) {
            return new RaceResult<>(value, null);
        }

        static <T> RaceResult<T> failure(RuntimeException error) {
            return new RaceResult<>(null, error);
        }

        boolean succeeded() {
            return error == null;
        }
    }
}
