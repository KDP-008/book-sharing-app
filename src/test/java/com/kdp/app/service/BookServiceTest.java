package com.kdp.app.service;

import com.kdp.app.dto.BookResponse;
import com.kdp.app.dto.CreateBookRequest;
import com.kdp.app.dto.RegisterUserRequest;
import com.kdp.app.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BookServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private BookService bookService;

    @Autowired
    private UserService userService;

    @Autowired
    private CartService cartService;

    @Test
    void searchAndOwnership_shouldWork() {
        UserResponse owner = authService.register(new RegisterUserRequest("Owner User", "owner@example.com", "OwnerPass123", "Memorable1", null, null, null));
        bookService.addBook(new CreateBookRequest(owner.getId(), "Clean Code", "Robert C. Martin", "Programming", true));
        bookService.addBook(new CreateBookRequest(owner.getId(), "Effective Java", "Joshua Bloch", "Programming", true));
        bookService.addBook(new CreateBookRequest(owner.getId(), "The Hobbit", "J.R.R. Tolkien", "Fantasy", true));

        List<BookResponse> byTitle = bookService.searchBooks("Clean", null, null);
        assertEquals(1, byTitle.size());
        assertEquals("Clean Code", byTitle.get(0).getTitle());

        List<BookResponse> byAuthor = bookService.searchBooks(null, "Tolkien", null);
        assertEquals(1, byAuthor.size());
        assertEquals("The Hobbit", byAuthor.get(0).getTitle());

        List<BookResponse> byGenre = bookService.searchBooks(null, null, "Programming");
        assertEquals(2, byGenre.size());

        List<BookResponse> ownedByUser = userService.getBooksOwnedByUser(owner.getId());
        assertEquals(3, ownedByUser.size());
    }

    @Test
    void returnBook_shouldMakeBookAvailableAndDeactivateRecord() {
        UserResponse owner = authService.register(new RegisterUserRequest("Owner", "owner_ret@example.com", "Pass1", "Secret1", null, null, null));
        UserResponse borrower = authService.register(new RegisterUserRequest("Borrower", "borrower_ret@example.com", "Pass2", "Secret2", null, null, null));

        BookResponse book = bookService.addBook(new CreateBookRequest(owner.getId(), "Refactoring", "Martin Fowler", "Programming", true));

        cartService.addToCart(borrower.getId(), book.getId());
        cartService.checkout(borrower.getId(), "COURIER");

        assertFalse(bookService.getBookById(book.getId()).isAvailable());

        BookResponse returned = bookService.returnBook(book.getId(), borrower.getId());
        assertTrue(returned.isAvailable());
        assertTrue(bookService.getBookById(book.getId()).isAvailable());
    }

    @Test
    void returnBook_shouldFail_whenNotBorrowed() {
        UserResponse owner = authService.register(new RegisterUserRequest("Owner", "owner_fail@example.com", "Pass1", "Secret1", null, null, null));
        UserResponse other = authService.register(new RegisterUserRequest("Other", "other_fail@example.com", "Pass2", "Secret2", null, null, null));
        BookResponse book = bookService.addBook(new CreateBookRequest(owner.getId(), "Design Patterns", "GoF", "Programming", true));

        assertThrows(IllegalArgumentException.class, () -> bookService.returnBook(book.getId(), other.getId()));
    }
}
