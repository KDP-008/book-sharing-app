package com.kdp.app.controller;

import com.kdp.app.model.CartItem;
import com.kdp.app.service.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/cart")
@Tag(name = "Cart", description = "Cart and checkout operations")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get cart items for a user")
    public ResponseEntity<List<CartItem>> getCart(@PathVariable Long userId) {
        return ResponseEntity.ok(cartService.getCartItems(userId));
    }

    @PostMapping("/{userId}/add")
    @Operation(summary = "Add a book to user's cart")
    public ResponseEntity<?> addToCart(@PathVariable Long userId, @RequestBody Map<String, Long> payload) {
        try {
            CartItem item = cartService.addToCart(userId, payload.get("bookId"));
            return ResponseEntity.ok(item);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/{userId}/checkout")
    @Operation(summary = "Checkout cart for a user")
    public ResponseEntity<?> checkout(@PathVariable Long userId, @RequestBody Map<String, String> payload) {
        try {
            var result = cartService.checkout(userId, payload.getOrDefault("deliveryMethod", "COURIER"));
            return ResponseEntity.ok(Map.of(
                    "message", "Checkout successful",
                    "deliveryMethod", result.getDeliveryMethod().name(),
                    "borrowedBooks", result.getBorrowingRecords().size()
            ));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }
}
