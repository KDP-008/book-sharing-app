package com.bsa.service;

import com.bsa.dto.BookWaitlistDemand;
import com.bsa.dto.WaitlistAnalytics;
import com.bsa.model.Book;
import com.bsa.model.BorrowingRecord;
import com.bsa.model.Reservation;
import com.bsa.model.ReservationAuditEvent;
import com.bsa.model.ReservationAuditLog;
import com.bsa.model.ReservationStatus;
import com.bsa.model.User;
import com.bsa.repository.ReservationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ReservationServiceTest {

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
    void reserveBook_shouldFail_whenBookIsAvailable() {
        User owner = authService.register("Owner", "owner-avail@example.com", "Pass123");
        User user = authService.register("User", "user-avail@example.com", "Pass123");
        Book book = bookService.addBook(owner.getId(), "Available Book", "Some Author", "Fiction", true);

        assertThrows(IllegalStateException.class,
                () -> reservationService.reserveBook(user.getId(), book.getId()));
    }

    @Test
    void reserveBook_shouldFail_forOwnBook() {
        User owner = authService.register("Owner", "owner-self@example.com", "Pass123");
        Book book = bookService.addBook(owner.getId(), "Owner Book", "Some Author", "Fiction", true);
        borrowOut(owner, book);

        assertThrows(IllegalArgumentException.class,
                () -> reservationService.reserveBook(owner.getId(), book.getId()));
    }

    @Test
    void reserveBook_shouldQueueAndPromoteInOrder_whenBookIsReturned() {
        User owner = authService.register("Owner", "owner-queue@example.com", "Pass123");
        User firstBorrower = authService.register("First Borrower", "first@example.com", "Pass123");
        User waiter1 = authService.register("Waiter One", "waiter1@example.com", "Pass123");
        User waiter2 = authService.register("Waiter Two", "waiter2@example.com", "Pass123");

        Book book = bookService.addBook(owner.getId(), "Popular Book", "Some Author", "Fiction", true);
        borrowOut(firstBorrower, book);

        // Duplicate reservations by the same user aren't allowed.
        Reservation reservation1 = reservationService.reserveBook(waiter1.getId(), book.getId());
        assertThrows(IllegalStateException.class,
                () -> reservationService.reserveBook(waiter1.getId(), book.getId()));

        Reservation reservation2 = reservationService.reserveBook(waiter2.getId(), book.getId());

        List<Reservation> queue = reservationService.getQueueForBook(book.getId());
        assertEquals(2, queue.size());
        assertEquals(reservation1.getId(), queue.get(0).getId());
        assertEquals(reservation2.getId(), queue.get(1).getId());

        bookService.returnBook(book.getId(), firstBorrower.getId());

        Reservation promoted = reservationService.getUserReservations(waiter1.getId()).get(0);
        assertEquals(ReservationStatus.READY_FOR_PICKUP, promoted.getStatus());
        assertNotNull(promoted.getExpiresAt());
        assertFalse(bookService.getBookById(book.getId()).isAvailable());
        assertEquals(1, reservationService.getQueueForBook(book.getId()).size());

        BorrowingRecord claimedRecord = reservationService.claimReservation(
                waiter1.getId(), promoted.getId(), "COURIER");
        assertEquals(waiter1.getId(), claimedRecord.getBorrower().getId());
        assertFalse(bookService.getBookById(book.getId()).isAvailable());

        // Waiter two stays queued until waiter one, who just claimed the book, returns it.
        Reservation waiter2StillWaiting = reservationService.getUserReservations(waiter2.getId()).get(0);
        assertEquals(ReservationStatus.WAITING, waiter2StillWaiting.getStatus());

        bookService.returnBook(book.getId(), waiter1.getId());

        Reservation waiter2Promoted = reservationService.getUserReservations(waiter2.getId()).get(0);
        assertEquals(ReservationStatus.READY_FOR_PICKUP, waiter2Promoted.getStatus());
    }

    @Test
    void returnBook_shouldMakeBookAvailable_whenNoOneIsWaiting() {
        User owner = authService.register("Owner", "owner-noqueue@example.com", "Pass123");
        User borrower = authService.register("Borrower", "borrower-noqueue@example.com", "Pass123");
        Book book = bookService.addBook(owner.getId(), "Quiet Book", "Some Author", "Fiction", true);
        borrowOut(borrower, book);

        bookService.returnBook(book.getId(), borrower.getId());

        assertTrue(bookService.getBookById(book.getId()).isAvailable());
    }

    @Test
    void cancelReservation_shouldPromoteNext_whenCancellingAReadySlot() {
        User owner = authService.register("Owner", "owner-cancel@example.com", "Pass123");
        User firstBorrower = authService.register("First Borrower", "first-cancel@example.com", "Pass123");
        User waiter1 = authService.register("Waiter One", "waiter1-cancel@example.com", "Pass123");
        User waiter2 = authService.register("Waiter Two", "waiter2-cancel@example.com", "Pass123");

        Book book = bookService.addBook(owner.getId(), "Contested Book", "Some Author", "Fiction", true);
        borrowOut(firstBorrower, book);

        Reservation reservation1 = reservationService.reserveBook(waiter1.getId(), book.getId());
        reservationService.reserveBook(waiter2.getId(), book.getId());
        bookService.returnBook(book.getId(), firstBorrower.getId());

        reservationService.cancelReservation(waiter1.getId(), reservation1.getId());

        Reservation waiter2Reservation = reservationService.getUserReservations(waiter2.getId()).get(0);
        assertEquals(ReservationStatus.READY_FOR_PICKUP, waiter2Reservation.getStatus());
    }

    @Test
    void claimReservation_shouldFail_whenNotOwnedByCaller() {
        User owner = authService.register("Owner", "owner-wrong@example.com", "Pass123");
        User firstBorrower = authService.register("First Borrower", "first-wrong@example.com", "Pass123");
        User waiter = authService.register("Waiter", "waiter-wrong@example.com", "Pass123");
        User intruder = authService.register("Intruder", "intruder-wrong@example.com", "Pass123");

        Book book = bookService.addBook(owner.getId(), "Guarded Book", "Some Author", "Fiction", true);
        borrowOut(firstBorrower, book);
        Reservation reservation = reservationService.reserveBook(waiter.getId(), book.getId());
        bookService.returnBook(book.getId(), firstBorrower.getId());

        assertThrows(IllegalArgumentException.class,
                () -> reservationService.claimReservation(intruder.getId(), reservation.getId(), "COURIER"));
    }

    @Test
    void getQueuePosition_andWaitlistDepth_shouldReflectQueueChanges() {
        User owner = authService.register("Owner", "owner-position@example.com", "Pass123");
        User firstBorrower = authService.register("First Borrower", "first-position@example.com", "Pass123");
        User waiter1 = authService.register("Waiter One", "waiter1-position@example.com", "Pass123");
        User waiter2 = authService.register("Waiter Two", "waiter2-position@example.com", "Pass123");
        User waiter3 = authService.register("Waiter Three", "waiter3-position@example.com", "Pass123");

        Book book = bookService.addBook(owner.getId(), "Ranked Book", "Some Author", "Fiction", true);
        borrowOut(firstBorrower, book);

        Reservation reservation1 = reservationService.reserveBook(waiter1.getId(), book.getId());
        Reservation reservation2 = reservationService.reserveBook(waiter2.getId(), book.getId());
        Reservation reservation3 = reservationService.reserveBook(waiter3.getId(), book.getId());

        assertEquals(1, reservationService.getQueuePosition(waiter1.getId(), book.getId()));
        assertEquals(2, reservationService.getQueuePosition(waiter2.getId(), book.getId()));
        assertEquals(3, reservationService.getQueuePosition(waiter3.getId(), book.getId()));
        assertEquals(3, reservationService.getWaitlistDepth(book.getId()));

        reservationService.cancelReservation(waiter2.getId(), reservation2.getId());

        assertEquals(1, reservationService.getQueuePosition(waiter1.getId(), book.getId()));
        assertEquals(2, reservationService.getQueuePosition(waiter3.getId(), book.getId()));
        assertEquals(2, reservationService.getWaitlistDepth(book.getId()));

        bookService.returnBook(book.getId(), firstBorrower.getId());

        assertEquals(0, reservationService.getQueuePosition(waiter1.getId(), book.getId()));
        assertEquals(1, reservationService.getQueuePosition(waiter3.getId(), book.getId()));
        assertEquals(1, reservationService.getWaitlistDepth(book.getId()));

        assertThrows(IllegalStateException.class,
                () -> reservationService.getQueuePosition(waiter2.getId(), book.getId()));
    }

    @Test
    void getWaitlistDepth_shouldBeZero_whenNoOneIsWaiting() {
        User owner = authService.register("Owner", "owner-depth@example.com", "Pass123");
        Book book = bookService.addBook(owner.getId(), "Lonely Book", "Some Author", "Fiction", true);

        assertEquals(0, reservationService.getWaitlistDepth(book.getId()));
    }

    @Test
    void getWaitlistAnalytics_shouldRankBooksAndBreakDownStatusesInTheDatabase() {
        User ownerA = authService.register("Owner A", "owner-a-analytics@example.com", "Pass123");
        User ownerB = authService.register("Owner B", "owner-b-analytics@example.com", "Pass123");
        User ownerC = authService.register("Owner C", "owner-c-analytics@example.com", "Pass123");
        User ownerD = authService.register("Owner D", "owner-d-analytics@example.com", "Pass123");
        User borrowerA = authService.register("Borrower A", "borrower-a-analytics@example.com", "Pass123");
        User borrowerB = authService.register("Borrower B", "borrower-b-analytics@example.com", "Pass123");
        User borrowerC = authService.register("Borrower C", "borrower-c-analytics@example.com", "Pass123");
        User borrowerD = authService.register("Borrower D", "borrower-d-analytics@example.com", "Pass123");
        User u1 = authService.register("Waiter U1", "u1-analytics@example.com", "Pass123");
        User u2 = authService.register("Waiter U2", "u2-analytics@example.com", "Pass123");
        User u3 = authService.register("Waiter U3", "u3-analytics@example.com", "Pass123");
        User u4 = authService.register("Waiter U4", "u4-analytics@example.com", "Pass123");
        User u5 = authService.register("Waiter U5", "u5-analytics@example.com", "Pass123");

        Book bookA = bookService.addBook(ownerA.getId(), "Most Wanted Book", "Author A", "Fiction", true);
        Book bookB = bookService.addBook(ownerB.getId(), "Somewhat Wanted Book", "Author B", "Fiction", true);
        Book bookC = bookService.addBook(ownerC.getId(), "Cancelled Interest Book", "Author C", "Fiction", true);
        Book bookD = bookService.addBook(ownerD.getId(), "Already Claimed Book", "Author D", "Fiction", true);

        borrowOut(borrowerA, bookA);
        borrowOut(borrowerB, bookB);
        borrowOut(borrowerC, bookC);
        borrowOut(borrowerD, bookD);

        reservationService.reserveBook(u1.getId(), bookA.getId());
        reservationService.reserveBook(u2.getId(), bookA.getId());
        reservationService.reserveBook(u3.getId(), bookB.getId());

        Reservation cancelled = reservationService.reserveBook(u4.getId(), bookC.getId());
        reservationService.cancelReservation(u4.getId(), cancelled.getId());

        Reservation toClaim = reservationService.reserveBook(u5.getId(), bookD.getId());
        bookService.returnBook(bookD.getId(), borrowerD.getId());
        reservationService.claimReservation(u5.getId(), toClaim.getId(), "COURIER");

        WaitlistAnalytics analytics = reservationService.getWaitlistAnalytics(10);

        List<BookWaitlistDemand> topRequestedBooks = analytics.topRequestedBooks();
        assertEquals(2, topRequestedBooks.size());
        assertEquals(bookA.getId(), topRequestedBooks.get(0).bookId());
        assertEquals(2, topRequestedBooks.get(0).waitlistCount());
        assertEquals(bookB.getId(), topRequestedBooks.get(1).bookId());
        assertEquals(1, topRequestedBooks.get(1).waitlistCount());

        var statusBreakdown = analytics.statusBreakdown();
        assertEquals(3L, statusBreakdown.get(ReservationStatus.WAITING));
        assertEquals(1L, statusBreakdown.get(ReservationStatus.CANCELLED));
        assertEquals(1L, statusBreakdown.get(ReservationStatus.FULFILLED));
        assertEquals(0L, statusBreakdown.get(ReservationStatus.READY_FOR_PICKUP));
        assertEquals(0L, statusBreakdown.get(ReservationStatus.EXPIRED));

        assertEquals(1, reservationService.getWaitlistAnalytics(1).topRequestedBooks().size());
        assertThrows(IllegalArgumentException.class, () -> reservationService.getWaitlistAnalytics(0));
    }

    @Test
    void auditLog_shouldRecordCreatedPromotedAndClaimedEvents() {
        User owner = authService.register("Owner", "owner-audit1@example.com", "Pass123");
        User firstBorrower = authService.register("First Borrower", "first-audit1@example.com", "Pass123");
        User waiter = authService.register("Waiter", "waiter-audit1@example.com", "Pass123");
        Book book = bookService.addBook(owner.getId(), "Audited Book", "Some Author", "Fiction", true);
        borrowOut(firstBorrower, book);

        Reservation reservation = reservationService.reserveBook(waiter.getId(), book.getId());
        bookService.returnBook(book.getId(), firstBorrower.getId());
        reservationService.claimReservation(waiter.getId(), reservation.getId(), "COURIER");

        List<ReservationAuditLog> history = reservationService.getAuditHistory(reservation.getId());
        assertEquals(3, history.size());

        ReservationAuditLog created = history.get(0);
        assertEquals(ReservationAuditEvent.CREATED, created.getTriggerEvent());
        assertNull(created.getOldStatus());
        assertEquals(ReservationStatus.WAITING, created.getNewStatus());
        assertEquals(book.getId(), created.getBookId());
        assertEquals(waiter.getId(), created.getUserId());
        assertEquals(reservation.getId(), created.getReservationId());

        ReservationAuditLog promoted = history.get(1);
        assertEquals(ReservationAuditEvent.PROMOTED, promoted.getTriggerEvent());
        assertEquals(ReservationStatus.WAITING, promoted.getOldStatus());
        assertEquals(ReservationStatus.READY_FOR_PICKUP, promoted.getNewStatus());

        ReservationAuditLog claimed = history.get(2);
        assertEquals(ReservationAuditEvent.CLAIMED, claimed.getTriggerEvent());
        assertEquals(ReservationStatus.READY_FOR_PICKUP, claimed.getOldStatus());
        assertEquals(ReservationStatus.FULFILLED, claimed.getNewStatus());
    }

    @Test
    void auditLog_shouldRecordCancelledEvent() {
        User owner = authService.register("Owner", "owner-audit2@example.com", "Pass123");
        User firstBorrower = authService.register("First Borrower", "first-audit2@example.com", "Pass123");
        User waiter = authService.register("Waiter", "waiter-audit2@example.com", "Pass123");
        Book book = bookService.addBook(owner.getId(), "Cancelled Audit Book", "Some Author", "Fiction", true);
        borrowOut(firstBorrower, book);

        Reservation reservation = reservationService.reserveBook(waiter.getId(), book.getId());
        reservationService.cancelReservation(waiter.getId(), reservation.getId());

        List<ReservationAuditLog> history = reservationService.getAuditHistory(reservation.getId());
        assertEquals(2, history.size());
        assertEquals(ReservationAuditEvent.CANCELLED, history.get(1).getTriggerEvent());
        assertEquals(ReservationStatus.WAITING, history.get(1).getOldStatus());
        assertEquals(ReservationStatus.CANCELLED, history.get(1).getNewStatus());
    }

    @Test
    void auditLog_shouldRecordExpiredEvent_andPromoteNextWaiter() {
        User owner = authService.register("Owner", "owner-audit3@example.com", "Pass123");
        User firstBorrower = authService.register("First Borrower", "first-audit3@example.com", "Pass123");
        User waiter1 = authService.register("Waiter One", "waiter1-audit3@example.com", "Pass123");
        User waiter2 = authService.register("Waiter Two", "waiter2-audit3@example.com", "Pass123");
        Book book = bookService.addBook(owner.getId(), "Expired Audit Book", "Some Author", "Fiction", true);
        borrowOut(firstBorrower, book);

        Reservation reservation1 = reservationService.reserveBook(waiter1.getId(), book.getId());
        reservationService.reserveBook(waiter2.getId(), book.getId());
        bookService.returnBook(book.getId(), firstBorrower.getId());

        // Force the claim window into the past to simulate an unclaimed reservation going stale.
        Reservation readyReservation = reservationRepository.findById(reservation1.getId()).orElseThrow();
        readyReservation.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        reservationRepository.save(readyReservation);

        reservationService.expireStaleReservations();

        List<ReservationAuditLog> history = reservationService.getAuditHistory(reservation1.getId());
        assertEquals(3, history.size());
        assertEquals(ReservationAuditEvent.EXPIRED, history.get(2).getTriggerEvent());
        assertEquals(ReservationStatus.READY_FOR_PICKUP, history.get(2).getOldStatus());
        assertEquals(ReservationStatus.EXPIRED, history.get(2).getNewStatus());

        assertEquals(ReservationStatus.READY_FOR_PICKUP,
                reservationService.getUserReservations(waiter2.getId()).get(0).getStatus());
    }

    @Test
    void getAuditHistory_shouldFail_whenReservationDoesNotExist() {
        assertThrows(IllegalArgumentException.class, () -> reservationService.getAuditHistory(999999L));
    }

    private void borrowOut(User borrower, Book book) {
        cartService.addToCart(borrower.getId(), book.getId());
        cartService.checkout(borrower.getId(), "COURIER");
    }
}
