package com.kdp.app.controller;

import com.kdp.app.model.Book;
import com.kdp.app.model.Notification;
import com.kdp.app.model.User;
import com.kdp.app.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<User> getUser(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    @PutMapping("/{userId}/preferences")
    public ResponseEntity<?> updatePreferences(@PathVariable Long userId,
                                              @RequestBody Map<String, String> payload) {
        userService.updatePreferences(userId, payload.get("favoriteGenre"), payload.get("favoriteAuthor"));
        return ResponseEntity.ok(Map.of("message", "Preferences updated"));
    }

    @GetMapping("/{userId}/books-owned")
    public ResponseEntity<List<Book>> getBooksOwned(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getBooksOwnedByUser(userId));
    }

    @GetMapping("/{userId}/borrowers")
    public ResponseEntity<List<User>> getBorrowers(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getBorrowersOfUserBooks(userId));
    }

    @GetMapping("/{userId}/books-borrowed")
    public ResponseEntity<List<Book>> getBooksBorrowed(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getBooksBorrowedByUser(userId));
    }

    @GetMapping("/{userId}/notifications")
    public ResponseEntity<List<Notification>> getNotifications(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getNotifications(userId));
    }
}
