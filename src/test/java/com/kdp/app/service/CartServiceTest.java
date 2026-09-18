package com.kdp.app.service;

import com.kdp.app.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CartServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private BookService bookService;

    @Autowired
    private CartService cartService;

    @Autowired
    private UserService userService;

    @Test
    void addToCart_andCheckout_shouldBorrowBooksAndCreateInboxMessages() {
        UserResponse owner = authService.register(new RegisterUserRequest("Library Owner", "owner2@example.com", "OwnerPass123", "Secret1", null, null, null));
        UserResponse borrower = authService.register(new RegisterUserRequest("Borrower User", "borrower@example.com", "BorrowPass123", "Secret2", null, null, null));

        BookResponse book = bookService.addBook(new CreateBookRequest(owner.getId(), "Java Concurrency", "Brian Goetz", "Programming", true));
        BookResponse secondBook = bookService.addBook(new CreateBookRequest(owner.getId(), "The Alchemist", "Paulo Coelho", "Fiction", true));

        cartService.addToCart(borrower.getId(), book.getId());
        cartService.addToCart(borrower.getId(), secondBook.getId());

        CheckoutResponse checkoutResult = cartService.checkout(borrower.getId(), "COURIER");

        assertEquals(2, checkoutResult.getBorrowedBooks());
        assertEquals("COURIER", checkoutResult.getDeliveryMethod());
        assertEquals(0, cartService.getCartItems(borrower.getId()).size());
        assertFalse(bookService.getBookById(book.getId()).isAvailable());

        // Verify inbox messages were created for recipient
        List<MessageResponse> inbox = userService.getInboxMessages(borrower.getId());
        assertEquals(2, inbox.size());
        assertTrue(inbox.get(0).getSubject().contains("borrowed"));
    }

    @Test
    void addToCart_shouldRejectSelfBorrowing() {
        UserResponse owner = authService.register(new RegisterUserRequest("Self Owner", "self@example.com", "Pass123", "Secret", null, null, null));
        BookResponse book = bookService.addBook(new CreateBookRequest(owner.getId(), "My Own Book", "Author", "Genre", true));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> cartService.addToCart(owner.getId(), book.getId()));
        assertEquals("Cannot borrow your own book.", ex.getMessage());
    }

    @Test
    void addToCart_shouldRejectDuplicateBook() {
        UserResponse owner = authService.register(new RegisterUserRequest("Owner", "owner_dup@example.com", "Pass123", "Secret", null, null, null));
        UserResponse borrower = authService.register(new RegisterUserRequest("Borrower", "borrower_dup@example.com", "Pass123", "Secret", null, null, null));
        BookResponse book = bookService.addBook(new CreateBookRequest(owner.getId(), "Duplicate Book", "Author", "Genre", true));

        cartService.addToCart(borrower.getId(), book.getId());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> cartService.addToCart(borrower.getId(), book.getId()));
        assertEquals("Book is already in your cart.", ex.getMessage());
    }

    @Test
    void removeCartItem_shouldRemoveItem() {
        UserResponse owner = authService.register(new RegisterUserRequest("Owner", "owner_rem@example.com", "Pass123", "Secret", null, null, null));
        UserResponse borrower = authService.register(new RegisterUserRequest("Borrower", "borrower_rem@example.com", "Pass123", "Secret", null, null, null));
        BookResponse book = bookService.addBook(new CreateBookRequest(owner.getId(), "Remove Book", "Author", "Genre", true));

        CartItemResponse item = cartService.addToCart(borrower.getId(), book.getId());
        assertEquals(1, cartService.getCartItems(borrower.getId()).size());

        cartService.removeCartItem(borrower.getId(), item.getId());
        assertEquals(0, cartService.getCartItems(borrower.getId()).size());
    }
}
