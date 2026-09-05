package com.kdp.app.service;

import com.kdp.app.model.User;
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
        User user = authService.register("Alice Johnson", "alice@example.com", "StrongPass123");

        assertNotNull(user.getId());
        assertEquals("alice@example.com", user.getEmail());

        Optional<User> loggedIn = authService.login("alice@example.com", "StrongPass123");
        assertTrue(loggedIn.isPresent());
        assertEquals("Alice Johnson", loggedIn.get().getName());

        Optional<User> invalidLogin = authService.login("alice@example.com", "wrong-password");
        assertTrue(invalidLogin.isEmpty());
    }

    @Test
    void resetPassword_shouldUpdatePassword() {
        authService.register("Bob Smith", "bob@example.com", "OldPass123");

        User updatedUser = authService.resetPassword("bob@example.com", "NewPass456");

        assertEquals("NewPass456", updatedUser.getPassword());
        assertTrue(authService.login("bob@example.com", "NewPass456").isPresent());
    }

    @Test
    void login_shouldFailWhenUserDoesNotExist() {
        Optional<User> user = authService.login("missing@example.com", "any-password");
        assertTrue(user.isEmpty());
    }
}
