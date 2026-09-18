package com.kdp.app.controller;

import com.kdp.app.dto.BookResponse;
import com.kdp.app.dto.MessageResponse;
import com.kdp.app.dto.UpdatePreferencesRequest;
import com.kdp.app.dto.UserPreferenceResponse;
import com.kdp.app.dto.UserResponse;
import com.kdp.app.model.Notification;
import com.kdp.app.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@Tag(name = "Users", description = "User profile and related operations")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user by id")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserResponseById(userId));
    }

    @GetMapping("/{userId}/preferences")
    @Operation(summary = "Get user preferences")
    public ResponseEntity<UserPreferenceResponse> getPreferences(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getPreferences(userId));
    }

    @PutMapping("/{userId}/preferences")
    @Operation(summary = "Update user preferences")
    public ResponseEntity<UserPreferenceResponse> updatePreferences(@PathVariable Long userId,
                                                                   @RequestBody UpdatePreferencesRequest request) {
        return ResponseEntity.ok(userService.updatePreferences(userId, request));
    }

    @GetMapping("/{userId}/books-owned")
    @Operation(summary = "List books owned by user")
    public ResponseEntity<List<BookResponse>> getBooksOwned(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getBooksOwnedByUser(userId));
    }

    @GetMapping("/{userId}/borrowers")
    @Operation(summary = "List users who borrowed this user's books")
    public ResponseEntity<List<UserResponse>> getBorrowers(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getBorrowersOfUserBooks(userId));
    }

    @GetMapping("/{userId}/books-borrowed")
    @Operation(summary = "List books borrowed by user")
    public ResponseEntity<List<BookResponse>> getBooksBorrowed(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getBooksBorrowedByUser(userId));
    }

    @GetMapping("/{userId}/notifications")
    @Operation(summary = "Get user notifications")
    public ResponseEntity<List<Notification>> getNotifications(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getNotifications(userId));
    }

    @GetMapping("/{userId}/inbox")
    @Operation(summary = "Get user inbox messages")
    public ResponseEntity<List<MessageResponse>> getInbox(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getInboxMessages(userId));
    }
}
