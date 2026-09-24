package com.bsa.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request payload to claim a reservation that is ready for pickup")
public record ClaimReservationRequest(
        @Schema(description = "Id of the user claiming the reservation", example = "1") Long userId,
        @Schema(description = "Delivery method for the resulting loan", example = "COURIER") String deliveryMethod) {
}
