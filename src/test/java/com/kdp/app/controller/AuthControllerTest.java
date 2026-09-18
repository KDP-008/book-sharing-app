package com.kdp.app.controller;

import com.kdp.app.dto.*;
import com.kdp.app.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest {

    private MockMvc mockMvc;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService)).build();
    }

    @Test
    void registerAndLogin_shouldWorkThroughApi() throws Exception {
        UserResponse userResponse = new UserResponse(1L, "Test User", "api-user@example.com", "N", "N", LocalDateTime.now(), LocalDateTime.now());
        LoginResponse loginResponse = new LoginResponse("test-token-123", true, userResponse);

        when(authService.register(any(RegisterUserRequest.class))).thenReturn(userResponse);
        when(authService.login(any(LoginRequest.class))).thenReturn(Optional.of(loginResponse));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test User\",\"email\":\"api-user@example.com\",\"password\":\"Pass123\",\"memorableInfo\":\"SecretWord\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("api-user@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.memorableInfo").doesNotExist());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"api-user@example.com\",\"password\":\"Pass123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("test-token-123"))
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.user.email").value("api-user@example.com"));
    }

    @Test
    void login_shouldReturn401_forInvalidCredentials() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(Optional.empty());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"wrong@example.com\",\"password\":\"WrongPass\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void resetPassword_shouldWorkThroughApi() throws Exception {
        UserResponse updatedUser = new UserResponse(1L, "Test User", "api-user@example.com", "N", "N", LocalDateTime.now(), LocalDateTime.now());
        when(authService.resetPassword(any(PasswordResetRequest.class))).thenReturn(updatedUser);

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"api-user@example.com\",\"memorableInfo\":\"SecretWord\",\"newPassword\":\"NewPass456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("api-user@example.com"));
    }
}
