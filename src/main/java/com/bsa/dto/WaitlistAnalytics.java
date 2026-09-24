package com.bsa.dto;

import com.bsa.model.ReservationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "Waitlist demand analytics for the library dashboard")
public record WaitlistAnalytics(
        @Schema(description = "Books with the deepest waitlists, ordered highest demand first")
        List<BookWaitlistDemand> topRequestedBooks,
        @Schema(description = "Reservation counts grouped by status; every status is present, defaulting to 0")
        Map<ReservationStatus, Long> statusBreakdown) {
}
