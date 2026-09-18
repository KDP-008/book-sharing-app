package com.kdp.app.service;

import com.kdp.app.dto.LoginRequest;
import com.kdp.app.dto.LoginResponse;
import com.kdp.app.dto.PasswordResetRequest;
import com.kdp.app.dto.RegisterUserRequest;
import com.kdp.app.dto.UserResponse;
import com.kdp.app.model.User;
import com.kdp.app.model.UserPreference;
import com.kdp.app.repository.UserPreferenceRepository;
import com.kdp.app.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       UserPreferenceRepository userPreferenceRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userPreferenceRepository = userPreferenceRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse register(RegisterUserRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Name is required.");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required.");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required.");
        }
        if (request.getMemorableInfo() == null || request.getMemorableInfo().isBlank()) {
            throw new IllegalArgumentException("Memorable info is required.");
        }

        if (userRepository.findByEmail(request.getEmail().trim()).isPresent()) {
            throw new IllegalArgumentException("User already exists with this email.");
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());
        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPassword(hashedPassword);
        user.setMemorableInfo(request.getMemorableInfo().trim());
        user.setIsLocked("N");
        user.setIsActive("Y");
        user.setCreateDate(LocalDateTime.now());
        user.setModifiedDate(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        if ((request.getFavoriteGenre() != null && !request.getFavoriteGenre().isBlank()) ||
            (request.getFavoriteAuthor() != null && !request.getFavoriteAuthor().isBlank()) ||
            (request.getFavoriteBook() != null && !request.getFavoriteBook().isBlank())) {

            UserPreference preference = new UserPreference(
                    savedUser,
                    request.getFavoriteGenre() != null ? request.getFavoriteGenre().trim() : null,
                    request.getFavoriteAuthor() != null ? request.getFavoriteAuthor().trim() : null,
                    request.getFavoriteBook() != null ? request.getFavoriteBook().trim() : null
            );
            userPreferenceRepository.save(preference);
            savedUser.setPreference(preference);
        }

        return UserResponse.from(savedUser);
    }

    public UserResponse register(String name, String email, String password) {
        return register(new RegisterUserRequest(name, email, password, "defaultMemorableInfo", null, null, null));
    }

    public Optional<LoginResponse> login(LoginRequest request) {
        if (request.getEmail() == null || request.getPassword() == null) {
            return Optional.empty();
        }

        return userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .filter(user -> passwordEncoder.matches(request.getPassword(), user.getPassword()))
                .map(user -> {
                    String token = UUID.randomUUID().toString();
                    return new LoginResponse(token, true, UserResponse.from(user));
                });
    }

    public Optional<UserResponse> login(String email, String password) {
        return login(new LoginRequest(email, password))
                .map(LoginResponse::getUser);
    }

    @Transactional
    public UserResponse resetPassword(PasswordResetRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required.");
        }
        if (request.getMemorableInfo() == null || request.getMemorableInfo().isBlank()) {
            throw new IllegalArgumentException("Memorable info is required.");
        }
        if (request.getNewPassword() == null || request.getNewPassword().isBlank()) {
            throw new IllegalArgumentException("New password is required.");
        }

        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("User not found for email: " + request.getEmail()));

        if (!user.getMemorableInfo().equalsIgnoreCase(request.getMemorableInfo().trim())) {
            throw new IllegalArgumentException("Invalid memorable info.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setModifiedDate(LocalDateTime.now());
        User savedUser = userRepository.save(user);

        return UserResponse.from(savedUser);
    }

    public UserResponse resetPassword(String email, String memorableInfo, String newPassword) {
        return resetPassword(new PasswordResetRequest(email, memorableInfo, newPassword));
    }
}
