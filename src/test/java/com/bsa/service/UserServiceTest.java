package com.bsa.service;

import com.bsa.model.Book;
import com.bsa.model.BorrowingRecord;
import com.bsa.model.DeliveryMethod;
import com.bsa.model.Notification;
import com.bsa.model.User;
import com.bsa.repository.BookRepository;
import com.bsa.repository.BorrowingRecordRepository;
import com.bsa.repository.NotificationRepository;
import com.bsa.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BorrowingRecordRepository borrowingRecordRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void getUserById_shouldReturnUserWhenPresent() {
        User user = new User("Alice", "alice@example.com", "secret");
        when(userRepository.findById(12L)).thenReturn(Optional.of(user));

        User result = userService.getUserById(12L);

        assertSame(user, result);
        verify(userRepository).findById(12L);
    }

    @Test
    void getUserById_shouldThrowWhenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.getUserById(99L));

        assertEquals("User not found with id: 99", exception.getMessage());
    }

    @Test
    void updatePreferences_shouldPersistUpdatedUserPreferences() {
        User user = new User("Alice", "alice@example.com", "secret");
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        userService.updatePreferences(7L, "Fantasy", "Tolkien");

        assertEquals("Fantasy", user.getFavoriteGenre());
        assertEquals("Tolkien", user.getFavoriteAuthor());
        verify(userRepository).save(user);
    }

    @Test
    void getBooksOwnedByUser_shouldReturnBooksOwnedByUser() {
        User owner = new User("Alice", "alice@example.com", "secret");
        Book bookOne = new Book("Dune", "Frank Herbert", "Sci-Fi", true, owner);
        Book bookTwo = new Book("The Hobbit", "J.R.R. Tolkien", "Fantasy", true, owner);
        when(userRepository.findById(3L)).thenReturn(Optional.of(owner));
        when(bookRepository.findByOwner(owner)).thenReturn(List.of(bookOne, bookTwo));

        List<Book> books = userService.getBooksOwnedByUser(3L);

        assertEquals(2, books.size());
        assertTrue(books.contains(bookOne));
        assertTrue(books.contains(bookTwo));
    }

    @Test
    void getBorrowersOfUserBooks_shouldReturnDistinctBorrowers() {
        User owner = new User("Alice", "alice@example.com", "secret");
        User borrowerOne = new User("Bob", "bob@example.com", "secret");
        User borrowerTwo = new User("Charlie", "charlie@example.com", "secret");
        Book bookOne = new Book("Dune", "Frank Herbert", "Sci-Fi", false, owner);
        Book bookTwo = new Book("The Hobbit", "J.R.R. Tolkien", "Fantasy", false, owner);

        BorrowingRecord recordOne = new BorrowingRecord(bookOne, borrowerOne, owner,
                LocalDate.now(), LocalDate.now().plusDays(7), DeliveryMethod.COURIER);
        BorrowingRecord recordTwo = new BorrowingRecord(bookTwo, borrowerOne, owner,
                LocalDate.now(), LocalDate.now().plusDays(7), DeliveryMethod.COURIER);
        BorrowingRecord recordThree = new BorrowingRecord(bookTwo, borrowerTwo, owner,
                LocalDate.now(), LocalDate.now().plusDays(10), DeliveryMethod.COURIER);

        when(userRepository.findById(5L)).thenReturn(Optional.of(owner));
        when(borrowingRecordRepository.findByOwner(owner)).thenReturn(List.of(recordOne, recordTwo, recordThree));

        List<User> borrowers = userService.getBorrowersOfUserBooks(5L);

        assertEquals(2, borrowers.size());
        assertTrue(borrowers.contains(borrowerOne));
        assertTrue(borrowers.contains(borrowerTwo));
    }

    @Test
    void getBooksBorrowedByUser_shouldReturnBorrowedBooks() {
        User borrower = new User("Bob", "bob@example.com", "secret");
        User owner = new User("Alice", "alice@example.com", "secret");
        Book bookOne = new Book("Dune", "Frank Herbert", "Sci-Fi", false, owner);
        Book bookTwo = new Book("The Hobbit", "J.R.R. Tolkien", "Fantasy", false, owner);

        BorrowingRecord recordOne = new BorrowingRecord(bookOne, borrower, owner,
                LocalDate.now(), LocalDate.now().plusDays(7), DeliveryMethod.COURIER);
        BorrowingRecord recordTwo = new BorrowingRecord(bookTwo, borrower, owner,
                LocalDate.now(), LocalDate.now().plusDays(10), DeliveryMethod.COURIER);

        when(userRepository.findById(9L)).thenReturn(Optional.of(borrower));
        when(borrowingRecordRepository.findByBorrower(borrower)).thenReturn(List.of(recordOne, recordTwo));

        List<Book> books = userService.getBooksBorrowedByUser(9L);

        assertEquals(2, books.size());
        assertTrue(books.contains(bookOne));
        assertTrue(books.contains(bookTwo));
    }

    @Test
    void getNotifications_shouldReturnNotificationsOrderedByDueDate() {
        User user = new User("Alice", "alice@example.com", "secret");
        Notification earlier = new Notification(user, "Return soon", LocalDate.now().plusDays(1));
        Notification later = new Notification(user, "Late return", LocalDate.now().plusDays(7));
        when(userRepository.findById(11L)).thenReturn(Optional.of(user));
        when(notificationRepository.findByUserOrderByDueDateAsc(user)).thenReturn(List.of(earlier, later));

        List<Notification> notifications = userService.getNotifications(11L);

        assertEquals(2, notifications.size());
        assertEquals(earlier, notifications.get(0));
        assertEquals(later, notifications.get(1));
    }

    @Test
    void sendReturnReminder_shouldCreateReminderNotification() {
        User borrower = new User("Bob", "bob@example.com", "secret");
        when(userRepository.findById(17L)).thenReturn(Optional.of(borrower));

        userService.sendReturnReminder(17L, 22L);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification savedNotification = captor.getValue();
        assertEquals(borrower, savedNotification.getUser());
        assertEquals("Return your borrowed book by the due date", savedNotification.getMessage());
        assertEquals(LocalDate.now().plusDays(7), savedNotification.getDueDate());
        assertFalse(savedNotification.isRead());
    }
}
