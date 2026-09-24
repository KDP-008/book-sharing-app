package com.bsa.controller;

import com.bsa.dto.CancelReservationRequest;
import com.bsa.dto.ClaimReservationRequest;
import com.bsa.dto.ReserveBookRequest;
import com.bsa.dto.WaitlistAnalytics;
import com.bsa.model.BorrowingRecord;
import com.bsa.model.Reservation;
import com.bsa.model.ReservationAuditLog;
import com.bsa.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/reservations")
@Tag(name = "Reservations", description = "Book waitlist and reservation operations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    @Operation(summary = "Reserve a book that is currently unavailable")
    public ResponseEntity<?> reserveBook(@RequestBody ReserveBookRequest request) {
        try {
            Reservation reservation = reservationService.reserveBook(request.userId(), request.bookId());
            return ResponseEntity.ok(reservation);
        } catch (IllegalArgumentException ex) {
            return badRequest(ex);
        } catch (IllegalStateException | DataIntegrityViolationException ex) {
            return conflict(ex);
        }
    }

    @PostMapping("/{reservationId}/claim")
    @Operation(summary = "Claim a reservation that is ready for pickup")
    public ResponseEntity<?> claimReservation(@PathVariable Long reservationId,
                                             @RequestBody ClaimReservationRequest request) {
        try {
            BorrowingRecord record = reservationService.claimReservation(
                    request.userId(), reservationId, request.deliveryMethod());
            return ResponseEntity.ok(record);
        } catch (IllegalArgumentException ex) {
            return badRequest(ex);
        } catch (IllegalStateException | DataIntegrityViolationException ex) {
            return conflict(ex);
        }
    }

    @PostMapping("/{reservationId}/cancel")
    @Operation(summary = "Cancel an active reservation")
    public ResponseEntity<?> cancelReservation(@PathVariable Long reservationId,
                                              @RequestBody CancelReservationRequest request) {
        try {
            reservationService.cancelReservation(request.userId(), reservationId);
            return ResponseEntity.ok(Map.of("message", "Reservation cancelled"));
        } catch (IllegalArgumentException ex) {
            return badRequest(ex);
        } catch (IllegalStateException | DataIntegrityViolationException ex) {
            return conflict(ex);
        }
    }

    @GetMapping("/queue-position")
    @Operation(summary = "Get a user's position in a book's waitlist")
    public ResponseEntity<?> getQueuePosition(@RequestParam Long userId, @RequestParam Long bookId) {
        try {
            int position = reservationService.getQueuePosition(userId, bookId);
            return ResponseEntity.ok(Map.of("position", position));
        } catch (IllegalArgumentException ex) {
            return badRequest(ex);
        } catch (IllegalStateException ex) {
            return conflict(ex);
        }
    }

    @GetMapping("/{reservationId}/audit-log")
    @Operation(summary = "Get the immutable status-change history for a reservation")
    public ResponseEntity<?> getAuditHistory(@PathVariable Long reservationId) {
        try {
            List<ReservationAuditLog> history = reservationService.getAuditHistory(reservationId);
            return ResponseEntity.ok(history);
        } catch (IllegalArgumentException ex) {
            return badRequest(ex);
        }
    }

    @GetMapping("/analytics")
    @Operation(summary = "Get waitlist demand analytics for the library dashboard")
    public ResponseEntity<?> getWaitlistAnalytics(@RequestParam(defaultValue = "10") int topBooksLimit) {
        try {
            WaitlistAnalytics analytics = reservationService.getWaitlistAnalytics(topBooksLimit);
            return ResponseEntity.ok(analytics);
        } catch (IllegalArgumentException ex) {
            return badRequest(ex);
        }
    }

    // Not found / bad input: the request refers to something that doesn't exist or isn't the
    // caller's to touch (unknown user/book/reservation, reservation owned by someone else).
    private ResponseEntity<Map<String, String>> badRequest(RuntimeException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }

    // Conflict: the request is well-formed but the current state won't allow it (already
    // reserved, not ready to claim, expired, or a raw DB constraint violation slipping through).
    private ResponseEntity<Map<String, String>> conflict(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", ex.getMessage()));
    }
}
