package com.kdp.app.dto;

import com.kdp.app.model.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Safe user profile representation without credentials")
public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private String isLocked;
    private String isActive;
    private LocalDateTime createDate;
    private LocalDateTime modifiedDate;

    public static UserResponse from(User user) {
        if (user == null) {
            return null;
        }
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getIsLocked(),
                user.getIsActive(),
                user.getCreateDate(),
                user.getModifiedDate()
        );
    }
}

