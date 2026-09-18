package com.kdp.app.dto;

import com.kdp.app.model.UserPreference;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User reading preferences")
public class UserPreferenceResponse {

    private Long userId;
    private String favoriteGenre;
    private String favoriteAuthor;
    private String favoriteBook;

    public static UserPreferenceResponse from(UserPreference preference) {
        if (preference == null) {
            return null;
        }
        Long uId = preference.getUser() != null ? preference.getUser().getId() : null;
        return new UserPreferenceResponse(
                uId,
                preference.getFavoriteGenre(),
                preference.getFavoriteAuthor(),
                preference.getFavoriteBook()
        );
    }
}

