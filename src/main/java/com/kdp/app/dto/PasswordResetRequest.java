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
@Schema(description = "Request payload to reset password using memorable info")
public class PasswordResetRequest {

    @Schema(description = "Registered email address", example = "alice@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @Schema(description = "Memorable info supplied during registration", example = "BlueSky", requiredMode = Schema.RequiredMode.REQUIRED)
    private String memorableInfo;

    @Schema(description = "New password to set", example = "NewPass5678!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String newPassword;
}

