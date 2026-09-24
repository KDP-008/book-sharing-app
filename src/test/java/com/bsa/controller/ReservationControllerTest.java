package com.bsa.controller;

import com.bsa.dto.BookWaitlistDemand;
import com.bsa.dto.WaitlistAnalytics;
import com.bsa.model.Book;
import com.bsa.model.BorrowingRecord;
import com.bsa.model.DeliveryMethod;
import com.bsa.model.Reservation;
import com.bsa.model.ReservationAuditEvent;
import com.bsa.model.ReservationAuditLog;
import com.bsa.model.ReservationStatus;
import com.bsa.model.User;
import com.bsa.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ReservationControllerTest {

    private MockMvc mockMvc;
    private ReservationService reservationService;

    private User owner;
    private User borrower;
    private Book book;

    @BeforeEach
    void setUp() {
        reservationService = mock(ReservationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ReservationController(reservationService)).build();

        owner = new User("Owner", "owner@example.com", "pass");
        owner.setId(1L);
        borrower = new User("Borrower", "borrower@example.com", "pass");
        borrower.setId(2L);
        book = new Book("Dune", "Frank Herbert", "Science Fiction", false, owner);
        book.setId(10L);
    }

    @Test
    void reserveBook_shouldReturn200_onSuccess() throws Exception {
        Reservation reservation = new Reservation(book, borrower, LocalDateTime.now());
        reservation.setId(5L);
        when(reservationService.reserveBook(2L, 10L)).thenReturn(reservation);

        mockMvc.perform(post("/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":2,\"bookId\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.status").value("WAITING"));
    }

    @Test
    void reserveBook_shouldReturn400_whenUserOrBookNotFound() throws Exception {
        when(reservationService.reserveBook(2L, 10L))
                .thenThrow(new IllegalArgumentException("Book not found with id: 10"));

        mockMvc.perform(post("/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":2,\"bookId\":10}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Book not found with id: 10"));
    }

    @Test
    void reserveBook_shouldReturn409_whenAlreadyReserved() throws Exception {
        when(reservationService.reserveBook(2L, 10L))
                .thenThrow(new IllegalStateException("You already have an active reservation for this book."));

        mockMvc.perform(post("/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":2,\"bookId\":10}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("You already have an active reservation for this book."));
    }

    @Test
    void reserveBook_shouldReturn409_onDatabaseConstraintViolation() throws Exception {
        when(reservationService.reserveBook(2L, 10L))
                .thenThrow(new DataIntegrityViolationException("uk_reservations_active_per_user violated"));

        mockMvc.perform(post("/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":2,\"bookId\":10}"))
                .andExpect(status().isConflict());
    }

    @Test
    void claimReservation_shouldReturn200_onSuccess() throws Exception {
        BorrowingRecord record = new BorrowingRecord(book, borrower, owner,
                LocalDate.now(), LocalDate.now().plusDays(14), DeliveryMethod.COURIER);
        record.setId(7L);
        when(reservationService.claimReservation(2L, 5L, "COURIER")).thenReturn(record);

        mockMvc.perform(post("/reservations/5/claim")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":2,\"deliveryMethod\":\"COURIER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.deliveryMethod").value("COURIER"));
    }

    @Test
    void claimReservation_shouldReturn409_whenNotReadyForPickup() throws Exception {
        when(reservationService.claimReservation(2L, 5L, "COURIER"))
                .thenThrow(new IllegalStateException("Reservation is not ready to be claimed."));

        mockMvc.perform(post("/reservations/5/claim")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":2,\"deliveryMethod\":\"COURIER\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Reservation is not ready to be claimed."));
    }

    @Test
    void claimReservation_shouldReturn400_whenReservationNotOwnedByCaller() throws Exception {
        when(reservationService.claimReservation(99L, 5L, "COURIER"))
                .thenThrow(new IllegalArgumentException("Reservation does not belong to this user."));

        mockMvc.perform(post("/reservations/5/claim")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":99,\"deliveryMethod\":\"COURIER\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancelReservation_shouldReturn200_onSuccess() throws Exception {
        mockMvc.perform(post("/reservations/5/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Reservation cancelled"));

        verify(reservationService).cancelReservation(2L, 5L);
    }

    @Test
    void cancelReservation_shouldReturn409_whenNotActive() throws Exception {
        doThrow(new IllegalStateException("Reservation is not active."))
                .when(reservationService).cancelReservation(2L, 5L);

        mockMvc.perform(post("/reservations/5/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":2}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Reservation is not active."));
    }

    @Test
    void getQueuePosition_shouldReturnPosition() throws Exception {
        when(reservationService.getQueuePosition(2L, 10L)).thenReturn(3);

        mockMvc.perform(get("/reservations/queue-position").param("userId", "2").param("bookId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.position").value(3));
    }

    @Test
    void getQueuePosition_shouldReturn409_whenNoActiveReservation() throws Exception {
        when(reservationService.getQueuePosition(2L, 10L))
                .thenThrow(new IllegalStateException("You do not have an active reservation for this book."));

        mockMvc.perform(get("/reservations/queue-position").param("userId", "2").param("bookId", "10"))
                .andExpect(status().isConflict());
    }

    @Test
    void getQueuePosition_shouldReturn400_whenBookNotFound() throws Exception {
        when(reservationService.getQueuePosition(2L, 10L))
                .thenThrow(new IllegalArgumentException("Book not found with id: 10"));

        mockMvc.perform(get("/reservations/queue-position").param("userId", "2").param("bookId", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getWaitlistAnalytics_shouldReturnAnalytics() throws Exception {
        BookWaitlistDemand demand = new BookWaitlistDemand(10L, "Dune", "Frank Herbert", 4L);
        Map<ReservationStatus, Long> statusBreakdown = new EnumMap<>(ReservationStatus.class);
        for (ReservationStatus status : ReservationStatus.values()) {
            statusBreakdown.put(status, 0L);
        }
        statusBreakdown.put(ReservationStatus.WAITING, 4L);
        when(reservationService.getWaitlistAnalytics(5))
                .thenReturn(new WaitlistAnalytics(List.of(demand), statusBreakdown));

        mockMvc.perform(get("/reservations/analytics").param("topBooksLimit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topRequestedBooks[0].title").value("Dune"))
                .andExpect(jsonPath("$.topRequestedBooks[0].waitlistCount").value(4))
                .andExpect(jsonPath("$.statusBreakdown.WAITING").value(4))
                .andExpect(jsonPath("$.statusBreakdown.CANCELLED").value(0));
    }

    @Test
    void getWaitlistAnalytics_shouldReturn400_forInvalidLimit() throws Exception {
        when(reservationService.getWaitlistAnalytics(0))
                .thenThrow(new IllegalArgumentException("topBooksLimit must be positive."));

        mockMvc.perform(get("/reservations/analytics").param("topBooksLimit", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAuditHistory_shouldReturnHistory() throws Exception {
        ReservationAuditLog created = new ReservationAuditLog(5L, 10L, 2L, null,
                ReservationStatus.WAITING, ReservationAuditEvent.CREATED, LocalDateTime.now());
        ReservationAuditLog cancelled = new ReservationAuditLog(5L, 10L, 2L, ReservationStatus.WAITING,
                ReservationStatus.CANCELLED, ReservationAuditEvent.CANCELLED, LocalDateTime.now());
        when(reservationService.getAuditHistory(5L)).thenReturn(List.of(created, cancelled));

        mockMvc.perform(get("/reservations/5/audit-log"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].triggerEvent").value("CREATED"))
                .andExpect(jsonPath("$[0].oldStatus").doesNotExist())
                .andExpect(jsonPath("$[0].newStatus").value("WAITING"))
                .andExpect(jsonPath("$[1].triggerEvent").value("CANCELLED"))
                .andExpect(jsonPath("$[1].oldStatus").value("WAITING"))
                .andExpect(jsonPath("$[1].newStatus").value("CANCELLED"));
    }

    @Test
    void getAuditHistory_shouldReturn400_whenReservationNotFound() throws Exception {
        when(reservationService.getAuditHistory(999L))
                .thenThrow(new IllegalArgumentException("Reservation not found with id: 999"));

        mockMvc.perform(get("/reservations/999/audit-log"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Reservation not found with id: 999"));
    }
}
