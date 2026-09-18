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
@Schema(description = "Request payload to register a new user")
public class RegisterUserRequest {

    @Schema(description = "User's full name", example = "Alice Smith", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "User's email address", example = "alice@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @Schema(description = "User's raw password", example = "Pass1234!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @Schema(description = "Memorable info for password recovery", example = "BlueSky", requiredMode = Schema.RequiredMode.REQUIRED)
    private String memorableInfo;

    @Schema(description = "Optional favorite genre", example = "Fantasy")
    private String favoriteGenre;

    @Schema(description = "Optional favorite author", example = "J.R.R. Tolkien")
    private String favoriteAuthor;

    @Schema(description = "Optional favorite book", example = "The Hobbit")
    private String favoriteBook;
}

