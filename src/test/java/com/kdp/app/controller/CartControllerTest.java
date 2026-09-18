package com.kdp.app.controller;

import com.kdp.app.dto.CartItemResponse;
import com.kdp.app.dto.CheckoutResponse;
import com.kdp.app.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CartControllerTest {

    private MockMvc mockMvc;
    private CartService cartService;

    @BeforeEach
    void setUp() {
        cartService = mock(CartService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new CartController(cartService)).build();
    }

    @Test
    void addToCartAndCheckout_shouldWorkThroughApi() throws Exception {
        CartItemResponse cartItemResponse = new CartItemResponse(99L, 10L, "Spring in Action", "Craig Walls", "Programming");
        CheckoutResponse checkoutResponse = new CheckoutResponse("Checkout successful", "COURIER", 1);

        when(cartService.addToCart(2L, 10L)).thenReturn(cartItemResponse);
        when(cartService.getCartItems(2L)).thenReturn(List.of(cartItemResponse));
        when(cartService.checkout(2L, "COURIER")).thenReturn(checkoutResponse);

        mockMvc.perform(post("/cart/2/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookId").value(10L));

        mockMvc.perform(post("/cart/2/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deliveryMethod\":\"COURIER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Checkout successful"));

        mockMvc.perform(get("/cart/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].bookTitle").value("Spring in Action"));
    }

    @Test
    void removeCartItem_shouldWorkThroughApi() throws Exception {
        doNothing().when(cartService).removeCartItem(2L, 99L);

        mockMvc.perform(delete("/cart/2/items/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Item removed from cart"));

        verify(cartService, times(1)).removeCartItem(2L, 99L);
    }
}
