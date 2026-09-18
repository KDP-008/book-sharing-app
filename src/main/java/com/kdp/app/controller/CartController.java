package com.kdp.app.controller;

import com.kdp.app.dto.AddToCartRequest;
import com.kdp.app.dto.CartItemResponse;
import com.kdp.app.dto.CheckoutRequest;
import com.kdp.app.dto.CheckoutResponse;
import com.kdp.app.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
    public ResponseEntity<List<CartItemResponse>> getCart(@PathVariable Long userId) {
        return ResponseEntity.ok(cartService.getCartItems(userId));
    }

    @PostMapping("/{userId}/add")
    @Operation(summary = "Add a book to user's cart")
    public ResponseEntity<CartItemResponse> addToCart(@PathVariable Long userId, @RequestBody AddToCartRequest request) {
        CartItemResponse item = cartService.addToCart(userId, request.getBookId());
        return ResponseEntity.ok(item);
    }

    @DeleteMapping("/{userId}/items/{cartItemId}")
    @Operation(summary = "Remove an item from user's cart")
    public ResponseEntity<Map<String, String>> removeCartItem(@PathVariable Long userId, @PathVariable Long cartItemId) {
        cartService.removeCartItem(userId, cartItemId);
        return ResponseEntity.ok(Map.of("message", "Item removed from cart"));
    }

    @PostMapping("/{userId}/checkout")
    @Operation(summary = "Checkout cart for a user")
    public ResponseEntity<CheckoutResponse> checkout(@PathVariable Long userId,
                                                      @RequestBody(required = false) CheckoutRequest request) {
        String deliveryMethod = (request != null && request.getDeliveryMethod() != null)
                ? request.getDeliveryMethod()
                : "COURIER";
        CheckoutResponse response = cartService.checkout(userId, deliveryMethod);
        return ResponseEntity.ok(response);
    }
}
