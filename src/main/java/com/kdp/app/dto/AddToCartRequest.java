package com.kdp.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload to add a book to cart")
public class AddToCartRequest {

    @Schema(description = "ID of the book to add", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long bookId;
}

