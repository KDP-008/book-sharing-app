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
@Schema(description = "Request payload to checkout cart")
public class CheckoutRequest {

    @Schema(description = "Delivery method (default COURIER)", example = "COURIER")
    private String deliveryMethod = "COURIER";
}

