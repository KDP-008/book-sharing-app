package com.kdp.app.service;

import com.kdp.app.model.Book;
import com.kdp.app.model.User;
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

    @Test
    void searchAndOwnership_shouldWork() {
        User owner = authService.register("Owner User", "owner@example.com", "OwnerPass123");
        bookService.addBook(owner.getId(), "Clean Code", "Robert C. Martin", "Programming", true);
        bookService.addBook(owner.getId(), "Effective Java", "Joshua Bloch", "Programming", true);
        bookService.addBook(owner.getId(), "The Hobbit", "J.R.R. Tolkien", "Fantasy", true);

        List<Book> byTitle = bookService.searchBooks("Clean", null, null);
        assertEquals(1, byTitle.size());
        assertEquals("Clean Code", byTitle.get(0).getTitle());

        List<Book> byAuthor = bookService.searchBooks(null, "Tolkien", null);
        assertEquals(1, byAuthor.size());
        assertEquals("The Hobbit", byAuthor.get(0).getTitle());

        List<Book> byGenre = bookService.searchBooks(null, null, "Programming");
        assertEquals(2, byGenre.size());

        List<Book> ownedByUser = userService.getBooksOwnedByUser(owner.getId());
        assertEquals(3, ownedByUser.size());
    }
}
