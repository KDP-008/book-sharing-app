package com.kdp.app.controller;

import com.kdp.app.dto.LoginRequest;
import com.kdp.app.dto.LoginResponse;
import com.kdp.app.dto.PasswordResetRequest;
import com.kdp.app.dto.RegisterUserRequest;
import com.kdp.app.dto.UserResponse;
import com.kdp.app.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

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
    public ResponseEntity<UserResponse> register(@RequestBody RegisterUserRequest request) {
        UserResponse user = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Optional<LoginResponse> response = authService.login(request);
        if (response.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid credentials"));
        }
        return ResponseEntity.ok(response.get());
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset a user's password using memorable info")
    public ResponseEntity<UserResponse> resetPassword(@RequestBody PasswordResetRequest request) {
        UserResponse user = authService.resetPassword(request);
        return ResponseEntity.ok(user);
    }
}
