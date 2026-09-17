package com.bsa.service;

import com.bsa.model.Book;
import com.bsa.model.BorrowingRecord;
import com.bsa.model.Notification;
import com.bsa.model.User;
import com.bsa.repository.BookRepository;
import com.bsa.repository.BorrowingRecordRepository;
import com.bsa.repository.NotificationRepository;
import com.bsa.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final BorrowingRecordRepository borrowingRecordRepository;
    private final NotificationRepository notificationRepository;

    public UserService(UserRepository userRepository,
                      BookRepository bookRepository,
                      BorrowingRecordRepository borrowingRecordRepository,
                      NotificationRepository notificationRepository) {
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.borrowingRecordRepository = borrowingRecordRepository;
        this.notificationRepository = notificationRepository;
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
    }

    public void updatePreferences(Long userId, String favoriteGenre, String favoriteAuthor) {
        User user = getUserById(userId);
        user.setFavoriteGenre(favoriteGenre);
        user.setFavoriteAuthor(favoriteAuthor);
        userRepository.save(user);
    }

    public List<Book> getBooksOwnedByUser(Long userId) {
        User user = getUserById(userId);
        return bookRepository.findByOwner(user);
    }

    public List<User> getBorrowersOfUserBooks(Long userId) {
        User owner = getUserById(userId);
        return borrowingRecordRepository.findByOwner(owner).stream()
                .map(BorrowingRecord::getBorrower)
                .distinct()
                .toList();
    }

    public List<Book> getBooksBorrowedByUser(Long userId) {
        User borrower = getUserById(userId);
        return borrowingRecordRepository.findByBorrower(borrower).stream()
                .map(BorrowingRecord::getBook)
                .toList();
    }

    public List<Notification> getNotifications(Long userId) {
        User user = getUserById(userId);
        return notificationRepository.findByUserOrderByDueDateAsc(user);
    }

    public void sendReturnReminder(Long borrowerId, Long bookId) {
        User borrower = getUserById(borrowerId);
        String message = "Return your borrowed book by the due date";
        Notification notification = new Notification(borrower, message, LocalDate.now().plusDays(7));
        notificationRepository.save(notification);
    }
}
