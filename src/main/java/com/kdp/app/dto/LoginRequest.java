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
@Schema(description = "Request payload to log in")
public class LoginRequest {

    @Schema(description = "User's email address", example = "alice@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @Schema(description = "User's password", example = "Pass1234!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;
}

