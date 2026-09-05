package com.kdp.app.controller;

import com.kdp.app.model.User;
import com.kdp.app.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Authentication and user account operations")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<?> register(@RequestBody Map<String, String> payload) {
        try {
            User user = authService.register(
                    payload.get("name"),
                    payload.get("email"),
                    payload.get("password")
            );
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public ResponseEntity<?> login(@RequestBody Map<String, String> payload) {
        Optional<User> user = authService.login(payload.get("email"), payload.get("password"));
        if (user.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid credentials"));
        }
        return ResponseEntity.ok(user.get());
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset a user's password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> payload) {
        try {
            User user = authService.resetPassword(payload.get("email"), payload.get("newPassword"));
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }
}
