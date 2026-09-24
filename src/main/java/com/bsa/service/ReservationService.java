package com.bsa.service;

import com.bsa.dto.BookWaitlistDemand;
import com.bsa.dto.ReservationStatusCount;
import com.bsa.dto.WaitlistAnalytics;
import com.bsa.model.*;
import com.bsa.repository.BookRepository;
import com.bsa.repository.BorrowingRecordRepository;
import com.bsa.repository.NotificationRepository;
import com.bsa.repository.ReservationAuditLogRepository;
import com.bsa.repository.ReservationRepository;
import com.bsa.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class ReservationService {

    private static final int CLAIM_WINDOW_DAYS = 3;
    private static final List<ReservationStatus> ACTIVE_STATUSES =
            List.of(ReservationStatus.WAITING, ReservationStatus.READY_FOR_PICKUP);

    private final ReservationRepository reservationRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final BorrowingRecordRepository borrowingRecordRepository;
    private final ReservationAuditLogRepository auditLogRepository;

    public ReservationService(ReservationRepository reservationRepository,
                             BookRepository bookRepository,
                             UserRepository userRepository,
                             NotificationRepository notificationRepository,
                             BorrowingRecordRepository borrowingRecordRepository,
                             ReservationAuditLogRepository auditLogRepository) {
        this.reservationRepository = reservationRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.borrowingRecordRepository = borrowingRecordRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public Reservation reserveBook(Long userId, Long bookId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        Book book = lockBook(bookId);

        if (book.isAvailable()) {
            throw new IllegalStateException("Book is currently available; borrow it instead of reserving it.");
        }
        if (book.getOwner().getId().equals(userId)) {
            throw new IllegalArgumentException("Owners cannot reserve their own books.");
        }
        if (reservationRepository.findByBookAndUserAndStatusIn(book, user, ACTIVE_STATUSES).isPresent()) {
            throw new IllegalStateException("You already have an active reservation for this book.");
        }

        Reservation reservation = new Reservation(book, user, LocalDateTime.now());
        Reservation saved;
        try {
            // Flush now (instead of at commit) so a concurrent insert that slips past the
            // checks above still trips uk_reservations_active_per_user inside this try block.
            saved = reservationRepository.saveAndFlush(reservation);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("You already have an active reservation for this book.");
        }

        recordAudit(saved, null, ReservationAuditEvent.CREATED);
        return saved;
    }

    @Transactional
    public void cancelReservation(Long userId, Long reservationId) {
        Book book = lockBookForReservation(reservationId);
        Reservation reservation = getOwnedReservation(userId, reservationId);
        if (!ACTIVE_STATUSES.contains(reservation.getStatus())) {
            throw new IllegalStateException("Reservation is not active.");
        }

        ReservationStatus oldStatus = reservation.getStatus();
        boolean wasHoldingSlot = oldStatus == ReservationStatus.READY_FOR_PICKUP;
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);
        recordAudit(reservation, oldStatus, ReservationAuditEvent.CANCELLED);

        if (wasHoldingSlot) {
            promoteNextInQueue(book);
        }
    }

    @Transactional
    public BorrowingRecord claimReservation(Long userId, Long reservationId, String deliveryMethod) {
        Book book = lockBookForReservation(reservationId);
        Reservation reservation = getOwnedReservation(userId, reservationId);

        if (reservation.getStatus() != ReservationStatus.READY_FOR_PICKUP) {
            throw new IllegalStateException("Reservation is not ready to be claimed.");
        }
        if (reservation.getExpiresAt().isBefore(LocalDateTime.now())) {
            expireReservation(reservation, book);
            throw new IllegalStateException("Reservation window has expired.");
        }

        DeliveryMethod method = DeliveryMethod.valueOf(deliveryMethod.toUpperCase());
        LocalDate borrowDate = LocalDate.now();
        LocalDate dueDate = borrowDate.plusDays(14);

        BorrowingRecord record = new BorrowingRecord(book, reservation.getUser(), book.getOwner(),
                borrowDate, dueDate, method);
        borrowingRecordRepository.save(record);

        reservation.setStatus(ReservationStatus.FULFILLED);
        reservation.setFulfilledAt(LocalDateTime.now());
        reservationRepository.save(reservation);
        recordAudit(reservation, ReservationStatus.READY_FOR_PICKUP, ReservationAuditEvent.CLAIMED);

        notificationRepository.save(new Notification(reservation.getUser(),
                "You claimed your reservation for '" + book.getTitle() + "'. Due date: " + dueDate,
                dueDate));

        return record;
    }

    // Callers must already hold the pessimistic lock on `book` (see BookService.returnBook).
    @Transactional
    public void handleBookReturned(Book book) {
        promoteNextInQueue(book);
    }

    public List<Reservation> getQueueForBook(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with id: " + bookId));
        return reservationRepository.findByBookAndStatusOrderByCreatedAtAsc(book, ReservationStatus.WAITING);
    }

    // 1-based position in the WAITING line, or 0 if the reservation is already READY_FOR_PICKUP.
    // Uses a COUNT query instead of loading the queue, so it's O(index lookup) not O(queue size).
    public int getQueuePosition(Long userId, Long bookId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with id: " + bookId));

        Reservation reservation = reservationRepository.findByBookAndUserAndStatusIn(book, user, ACTIVE_STATUSES)
                .orElseThrow(() -> new IllegalStateException("You do not have an active reservation for this book."));

        if (reservation.getStatus() == ReservationStatus.READY_FOR_PICKUP) {
            return 0;
        }

        long ahead = reservationRepository.countAheadInQueue(book, ReservationStatus.WAITING,
                reservation.getCreatedAt(), reservation.getId());
        return (int) ahead + 1;
    }

    // Number of users currently WAITING for a book (excludes whoever is READY_FOR_PICKUP).
    public long getWaitlistDepth(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with id: " + bookId));
        return reservationRepository.countByBookAndStatus(book, ReservationStatus.WAITING);
    }

    public List<Reservation> getUserReservations(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        return reservationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public List<ReservationAuditLog> getAuditHistory(Long reservationId) {
        if (!reservationRepository.existsById(reservationId)) {
            throw new IllegalArgumentException("Reservation not found with id: " + reservationId);
        }
        return auditLogRepository.findByReservationIdOrderByChangedAtAsc(reservationId);
    }

    // Both queries GROUP BY in the database (see ReservationRepository); this method only
    // reshapes their already-aggregated results, it does no counting of its own.
    public WaitlistAnalytics getWaitlistAnalytics(int topBooksLimit) {
        if (topBooksLimit <= 0) {
            throw new IllegalArgumentException("topBooksLimit must be positive.");
        }

        List<BookWaitlistDemand> topRequestedBooks = reservationRepository.findTopRequestedBooks(
                ReservationStatus.WAITING, PageRequest.of(0, topBooksLimit));

        Map<ReservationStatus, Long> statusBreakdown = new EnumMap<>(ReservationStatus.class);
        for (ReservationStatus status : ReservationStatus.values()) {
            statusBreakdown.put(status, 0L);
        }
        for (ReservationStatusCount count : reservationRepository.countReservationsByStatus()) {
            statusBreakdown.put(count.status(), count.count());
        }

        return new WaitlistAnalytics(topRequestedBooks, statusBreakdown);
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void expireStaleReservations() {
        List<Long> staleIds = reservationRepository
                .findByStatusAndExpiresAtBefore(ReservationStatus.READY_FOR_PICKUP, LocalDateTime.now())
                .stream()
                .map(Reservation::getId)
                .toList();

        for (Long reservationId : staleIds) {
            Book book = lockBookForReservation(reservationId);
            // Re-fetch under the lock: the reservation may have been claimed or cancelled
            // by the time we got here, and the bulk read above must not be trusted as current.
            Reservation reservation = reservationRepository.findById(reservationId).orElse(null);
            if (reservation == null
                    || reservation.getStatus() != ReservationStatus.READY_FOR_PICKUP
                    || reservation.getExpiresAt().isAfter(LocalDateTime.now())) {
                continue;
            }
            expireReservation(reservation, book);
        }
    }

    // Assumes the caller holds the pessimistic lock on `book` for the duration of this call.
    private void promoteNextInQueue(Book book) {
        List<Reservation> queue = reservationRepository.findByBookAndStatusOrderByCreatedAtAsc(
                book, ReservationStatus.WAITING);

        if (queue.isEmpty()) {
            book.setAvailable(true);
            bookRepository.save(book);
            return;
        }

        Reservation next = queue.get(0);
        LocalDateTime now = LocalDateTime.now();
        next.setStatus(ReservationStatus.READY_FOR_PICKUP);
        next.setReadyAt(now);
        next.setExpiresAt(now.plusDays(CLAIM_WINDOW_DAYS));
        reservationRepository.save(next);
        recordAudit(next, ReservationStatus.WAITING, ReservationAuditEvent.PROMOTED);

        book.setAvailable(false);
        bookRepository.save(book);

        // dueDate on Notification doubles as the claim deadline here.
        notificationRepository.save(new Notification(next.getUser(),
                "'" + book.getTitle() + "' is ready for you to claim. Claim it within "
                        + CLAIM_WINDOW_DAYS + " days.",
                next.getExpiresAt().toLocalDate()));
    }

    private void expireReservation(Reservation reservation, Book lockedBook) {
        reservation.setStatus(ReservationStatus.EXPIRED);
        reservationRepository.save(reservation);
        recordAudit(reservation, ReservationStatus.READY_FOR_PICKUP, ReservationAuditEvent.EXPIRED);
        promoteNextInQueue(lockedBook);
    }

    private void recordAudit(Reservation reservation, ReservationStatus oldStatus, ReservationAuditEvent event) {
        auditLogRepository.save(new ReservationAuditLog(
                reservation.getId(), reservation.getBook().getId(), reservation.getUser().getId(),
                oldStatus, reservation.getStatus(), event, LocalDateTime.now()));
    }

    private Reservation getOwnedReservation(Long userId, Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found with id: " + reservationId));
        if (!reservation.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Reservation does not belong to this user.");
        }
        return reservation;
    }

    private Book lockBook(Long bookId) {
        return bookRepository.findByIdForUpdate(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with id: " + bookId));
    }

    // Resolves the owning book and locks it *before* the reservation is loaded into the
    // persistence context, so the reservation we read afterward can't be a stale copy that
    // was cached before a concurrent transaction committed its changes.
    private Book lockBookForReservation(Long reservationId) {
        Long bookId = reservationRepository.findBookIdById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found with id: " + reservationId));
        return lockBook(bookId);
    }
}
