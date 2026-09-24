package com.bsa.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "reservations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_reservations_active_per_user",
                columnNames = {"book_id", "user_id", "active_flag"}),
        indexes = {
                @Index(name = "idx_reservations_book_status_created", columnList = "book_id, status, created_at"),
                @Index(name = "idx_reservations_status_expires", columnList = "status, expires_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Setter(AccessLevel.NONE)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status = ReservationStatus.WAITING;

    // Mirrors `status`: TRUE while WAITING/READY_FOR_PICKUP, NULL otherwise. NULLs are not
    // considered equal by unique constraints, so this lets uk_reservations_active_per_user
    // enforce "at most one active reservation per (book, user)" while still allowing unlimited
    // past (fulfilled/cancelled/expired) reservations for the same pair.
    @JsonIgnore
    @Column(name = "active_flag")
    private Boolean activeFlag = Boolean.TRUE;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime readyAt;

    private LocalDateTime expiresAt;

    private LocalDateTime fulfilledAt;

    public Reservation(Book book, User user, LocalDateTime createdAt) {
        this.book = book;
        this.user = user;
        this.createdAt = createdAt;
        setStatus(ReservationStatus.WAITING);
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
        this.activeFlag = isActive(status) ? Boolean.TRUE : null;
    }

    private static boolean isActive(ReservationStatus status) {
        return status == ReservationStatus.WAITING || status == ReservationStatus.READY_FOR_PICKUP;
    }
}
