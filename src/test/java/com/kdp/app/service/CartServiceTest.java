package com.kdp.app.service;

import com.kdp.app.model.Book;
import com.kdp.app.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

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

    @Test
    void addToCart_andCheckout_shouldBorrowBooks() {
        User owner = authService.register("Library Owner", "owner2@example.com", "OwnerPass123");
        User borrower = authService.register("Borrower User", "borrower@example.com", "BorrowPass123");

        Book book = bookService.addBook(owner.getId(), "Java Concurrency", "Brian Goetz", "Programming", true);
        Book secondBook = bookService.addBook(owner.getId(), "The Alchemist", "Paulo Coelho", "Fiction", true);

        cartService.addToCart(borrower.getId(), book.getId());
        cartService.addToCart(borrower.getId(), secondBook.getId());

        var checkoutResult = cartService.checkout(borrower.getId(), "COURIER");

        assertEquals(2, checkoutResult.getBorrowingRecords().size());
        assertEquals("COURIER", checkoutResult.getDeliveryMethod().name());
        assertEquals(0, cartService.getCartItems(borrower.getId()).size());
        assertFalse(bookService.getBookById(book.getId()).isAvailable());
    }
}
