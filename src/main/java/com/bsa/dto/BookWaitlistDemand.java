package com.bsa.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A book's current waitlist demand")
public record BookWaitlistDemand(
        @Schema(description = "Book id") Long bookId,
        @Schema(description = "Book title") String title,
        @Schema(description = "Book author") String author,
        @Schema(description = "Number of users currently waiting for this book") long waitlistCount) {
}
