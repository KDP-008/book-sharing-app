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
@Schema(description = "Request payload to update user reading preferences")
public class UpdatePreferencesRequest {

    @Schema(description = "Favorite book genre", example = "Science Fiction")
    private String favoriteGenre;

    @Schema(description = "Favorite author", example = "Isaac Asimov")
    private String favoriteAuthor;

    @Schema(description = "Favorite book title", example = "Foundation")
    private String favoriteBook;
}

