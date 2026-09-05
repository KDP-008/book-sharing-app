package com.kdp.app.controller;

import com.kdp.app.model.Book;
import com.kdp.app.model.CartItem;
import com.kdp.app.model.DeliveryMethod;
import com.kdp.app.model.User;
import com.kdp.app.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.*;
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
        User owner = new User("Owner", "owner@example.com", "pass");
        owner.setId(1L);
        User borrower = new User("Borrower", "borrower@example.com", "pass");
        borrower.setId(2L);
        Book book = new Book("Spring in Action", "Craig Walls", "Programming", true, owner);
        book.setId(10L);
        CartItem cartItem = new CartItem(99L, borrower, book);

        when(cartService.addToCart(2L, 10L)).thenReturn(cartItem);
        when(cartService.getCartItems(2L)).thenReturn(List.of());
        when(cartService.checkout(2L, "COURIER")).thenReturn(new CartService.CheckoutResult(List.of(), DeliveryMethod.COURIER));

        mockMvc.perform(post("/cart/2/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":10}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/cart/2/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deliveryMethod\":\"COURIER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Checkout successful"));

        mockMvc.perform(get("/cart/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
