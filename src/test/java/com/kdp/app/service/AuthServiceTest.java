package com.kdp.app.service;

import com.kdp.app.dto.LoginRequest;
import com.kdp.app.dto.LoginResponse;
import com.kdp.app.dto.PasswordResetRequest;
import com.kdp.app.dto.RegisterUserRequest;
import com.kdp.app.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Test
    void registerAndLogin_shouldWork() {
        RegisterUserRequest request = new RegisterUserRequest(
                "Alice Johnson",
                "alice@example.com",
                "StrongPass123",
                "SecretMemorable",
                "Fantasy",
                "J.R.R. Tolkien",
                "The Hobbit"
        );
        UserResponse user = authService.register(request);

        assertNotNull(user.getId());
        assertEquals("alice@example.com", user.getEmail());
        assertEquals("N", user.getIsLocked());
        assertEquals("Y", user.getIsActive());

        Optional<LoginResponse> loggedIn = authService.login(new LoginRequest("alice@example.com", "StrongPass123"));
        assertTrue(loggedIn.isPresent());
        assertTrue(loggedIn.get().isAuthenticated());
        assertNotNull(loggedIn.get().getToken());
        assertEquals("Alice Johnson", loggedIn.get().getUser().getName());

        Optional<LoginResponse> invalidLogin = authService.login(new LoginRequest("alice@example.com", "wrong-password"));
        assertTrue(invalidLogin.isEmpty());
    }

    @Test
    void resetPassword_shouldUpdatePassword_whenMemorableInfoMatches() {
        RegisterUserRequest request = new RegisterUserRequest(
                "Bob Smith",
                "bob@example.com",
                "OldPass123",
                "MyFirstPet",
                null, null, null
        );
        authService.register(request);

        PasswordResetRequest resetReq = new PasswordResetRequest("bob@example.com", "MyFirstPet", "NewPass456");
        UserResponse updatedUser = authService.resetPassword(resetReq);

        assertNotNull(updatedUser);
        assertEquals("bob@example.com", updatedUser.getEmail());
        assertTrue(authService.login(new LoginRequest("bob@example.com", "NewPass456")).isPresent());
    }

    @Test
    void resetPassword_shouldFail_whenMemorableInfoDoesNotMatch() {
        RegisterUserRequest request = new RegisterUserRequest(
                "Charlie Brown",
                "charlie@example.com",
                "OldPass123",
                "CorrectSecret",
                null, null, null
        );
        authService.register(request);

        PasswordResetRequest resetReq = new PasswordResetRequest("charlie@example.com", "WrongSecret", "NewPass456");
        assertThrows(IllegalArgumentException.class, () -> authService.resetPassword(resetReq));
    }

    @Test
    void login_shouldFailWhenUserDoesNotExist() {
        Optional<LoginResponse> user = authService.login(new LoginRequest("missing@example.com", "any-password"));
        assertTrue(user.isEmpty());
    }
}
