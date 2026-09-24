package com.bsa.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request payload to reserve an unavailable book")
public record ReserveBookRequest(
        @Schema(description = "Id of the user reserving the book", example = "1") Long userId,
        @Schema(description = "Id of the book to reserve", example = "10") Long bookId) {
}
