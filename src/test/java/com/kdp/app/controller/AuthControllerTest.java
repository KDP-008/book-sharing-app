package com.kdp.app.controller;

import com.kdp.app.model.User;
import com.kdp.app.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
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
        User user = new User(1L, "Test User", "api-user@example.com", "Pass123", null, null, null, null, null, null);
        when(authService.register("Test User", "api-user@example.com", "Pass123")).thenReturn(user);
        when(authService.login("api-user@example.com", "Pass123")).thenReturn(Optional.of(user));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test User\",\"email\":\"api-user@example.com\",\"password\":\"Pass123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("api-user@example.com"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"api-user@example.com\",\"password\":\"Pass123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("api-user@example.com"));
    }

    @Test
    void login_shouldReturn401_forInvalidCredentials() throws Exception {
        when(authService.login("wrong@example.com", "WrongPass")).thenReturn(Optional.empty());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"wrong@example.com\",\"password\":\"WrongPass\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }
}
