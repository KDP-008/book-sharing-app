package com.bsa.repository;

import com.bsa.dto.BookWaitlistDemand;
import com.bsa.dto.ReservationStatusCount;
import com.bsa.model.Book;
import com.bsa.model.Reservation;
import com.bsa.model.ReservationStatus;
import com.bsa.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByBookAndStatusOrderByCreatedAtAsc(Book book, ReservationStatus status);

    List<Reservation> findByUserOrderByCreatedAtDesc(User user);

    Optional<Reservation> findByBookAndUserAndStatusIn(Book book, User user, List<ReservationStatus> statuses);

    List<Reservation> findByStatusAndExpiresAtBefore(ReservationStatus status, LocalDateTime cutoff);

    // Scalar projection so callers can resolve which Book to lock before the Reservation
    // itself is loaded into the persistence context (see ReservationService.lockBookForReservation).
    @Query("SELECT r.book.id FROM Reservation r WHERE r.id = :id")
    Optional<Long> findBookIdById(@Param("id") Long id);

    // Total waitlist depth for a book. Backed by idx_reservations_book_status_created.
    long countByBookAndStatus(Book book, ReservationStatus status);

    // Number of same-status reservations ahead of (createdAt, id) in FIFO order, without
    // loading the queue into memory. Position = this count + 1.
    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.book = :book AND r.status = :status " +
           "AND (r.createdAt < :createdAt OR (r.createdAt = :createdAt AND r.id < :id))")
    long countAheadInQueue(@Param("book") Book book, @Param("status") ReservationStatus status,
                          @Param("createdAt") LocalDateTime createdAt, @Param("id") Long id);

    // Demand ranking for the dashboard: grouped and ordered in the database, then trimmed to
    // `pageable`'s page size (translates to a SQL LIMIT) so only the top N rows ever leave the DB.
    @Query("SELECT new com.bsa.dto.BookWaitlistDemand(r.book.id, r.book.title, r.book.author, COUNT(r)) " +
           "FROM Reservation r WHERE r.status = :status " +
           "GROUP BY r.book.id, r.book.title, r.book.author " +
           "ORDER BY COUNT(r) DESC")
    List<BookWaitlistDemand> findTopRequestedBooks(@Param("status") ReservationStatus status, Pageable pageable);

    @Query("SELECT new com.bsa.dto.ReservationStatusCount(r.status, COUNT(r)) " +
           "FROM Reservation r GROUP BY r.status")
    List<ReservationStatusCount> countReservationsByStatus();
}
