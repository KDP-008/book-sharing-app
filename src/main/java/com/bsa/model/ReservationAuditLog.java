package com.bsa.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// Append-only compliance record of a reservation's status transitions. Deliberately has no
// setters and no relation mappings (just raw ids) so a row, once written, can't be mutated by
// the app and doesn't get swept up by cascades on Book/User/Reservation.
@Entity
@Table(
        name = "reservation_audit_logs",
        indexes = @Index(name = "idx_audit_reservation_changed_at", columnList = "reservation_id, changed_at")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReservationAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reservation_id", nullable = false)
    private Long reservationId;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // Null only for the CREATED event, since a freshly queued reservation has no prior status.
    @Enumerated(EnumType.STRING)
    private ReservationStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus newStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationAuditEvent triggerEvent;

    @Column(nullable = false)
    private LocalDateTime changedAt;

    public ReservationAuditLog(Long reservationId, Long bookId, Long userId, ReservationStatus oldStatus,
                              ReservationStatus newStatus, ReservationAuditEvent triggerEvent,
                              LocalDateTime changedAt) {
        this.reservationId = reservationId;
        this.bookId = bookId;
        this.userId = userId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.triggerEvent = triggerEvent;
        this.changedAt = changedAt;
    }
}
