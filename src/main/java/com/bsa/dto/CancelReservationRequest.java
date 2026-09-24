package com.bsa.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request payload to cancel an active reservation")
public record CancelReservationRequest(
        @Schema(description = "Id of the user cancelling the reservation", example = "1") Long userId) {
}
