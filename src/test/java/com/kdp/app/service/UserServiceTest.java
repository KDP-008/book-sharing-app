package com.kdp.app.service;

import com.kdp.app.dto.BookResponse;
import com.kdp.app.dto.CreateBookRequest;
import com.kdp.app.dto.RegisterUserRequest;
import com.kdp.app.dto.UpdatePreferencesRequest;
import com.kdp.app.dto.UserPreferenceResponse;
import com.kdp.app.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UserServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @Autowired
    private BookService bookService;

    @Autowired
    private CartService cartService;

    @Test
    void userPreferences_shouldBeStoredAndRetrievedSeparately() {
        RegisterUserRequest reg = new RegisterUserRequest(
                "Reader One",
                "reader1@example.com",
                "Password123",
                "MemorableWord",
                "Science Fiction",
                "Arthur C. Clarke",
                "2001: A Space Odyssey"
        );
        UserResponse user = authService.register(reg);

        UserPreferenceResponse prefs = userService.getPreferences(user.getId());
        assertNotNull(prefs);
        assertEquals("Science Fiction", prefs.getFavoriteGenre());
        assertEquals("Arthur C. Clarke", prefs.getFavoriteAuthor());
        assertEquals("2001: A Space Odyssey", prefs.getFavoriteBook());

        // Update preferences
        UpdatePreferencesRequest updateReq = new UpdatePreferencesRequest("Cyberpunk", "William Gibson", "Neuromancer");
        UserPreferenceResponse updated = userService.updatePreferences(user.getId(), updateReq);

        assertEquals("Cyberpunk", updated.getFavoriteGenre());
        assertEquals("William Gibson", updated.getFavoriteAuthor());
        assertEquals("Neuromancer", updated.getFavoriteBook());
    }

    @Test
    void getBorrowersOfUserBooks_shouldReturnSanitizedUserResponses() {
        UserResponse owner = authService.register(new RegisterUserRequest("Owner", "owner_san@example.com", "Pass1", "Secret1", null, null, null));
        UserResponse borrower = authService.register(new RegisterUserRequest("Borrower", "borrower_san@example.com", "Pass2", "Secret2", null, null, null));

        BookResponse book = bookService.addBook(new CreateBookRequest(owner.getId(), "Clean Architecture", "Robert C. Martin", "Programming", true));
        cartService.addToCart(borrower.getId(), book.getId());
        cartService.checkout(borrower.getId(), "COURIER");

        List<UserResponse> borrowers = userService.getBorrowersOfUserBooks(owner.getId());
        assertEquals(1, borrowers.size());
        assertEquals("Borrower", borrowers.get(0).getName());
        assertEquals("borrower_san@example.com", borrowers.get(0).getEmail());
    }
}

