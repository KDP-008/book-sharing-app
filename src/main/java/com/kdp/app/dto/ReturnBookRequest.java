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
@Schema(description = "Request payload to return a borrowed book")
public class ReturnBookRequest {

    @Schema(description = "ID of the user returning the book", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long borrowerId;
}

