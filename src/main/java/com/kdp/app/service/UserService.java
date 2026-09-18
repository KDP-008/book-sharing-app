package com.kdp.app.service;

import com.kdp.app.dto.BookResponse;
import com.kdp.app.dto.MessageResponse;
import com.kdp.app.dto.UpdatePreferencesRequest;
import com.kdp.app.dto.UserPreferenceResponse;
import com.kdp.app.dto.UserResponse;
import com.kdp.app.model.BorrowingRecord;
import com.kdp.app.model.Notification;
import com.kdp.app.model.User;
import com.kdp.app.model.UserPreference;
import com.kdp.app.repository.BookRepository;
import com.kdp.app.repository.BorrowingRecordRepository;
import com.kdp.app.repository.MessageRepository;
import com.kdp.app.repository.NotificationRepository;
import com.kdp.app.repository.UserPreferenceRepository;
import com.kdp.app.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final BorrowingRecordRepository borrowingRecordRepository;
    private final NotificationRepository notificationRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final MessageRepository messageRepository;

    public UserService(UserRepository userRepository,
                       BookRepository bookRepository,
                       BorrowingRecordRepository borrowingRecordRepository,
                       NotificationRepository notificationRepository,
                       UserPreferenceRepository userPreferenceRepository,
                       MessageRepository messageRepository) {
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.borrowingRecordRepository = borrowingRecordRepository;
        this.notificationRepository = notificationRepository;
        this.userPreferenceRepository = userPreferenceRepository;
        this.messageRepository = messageRepository;
    }

    @Transactional(readOnly = true)
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
    }

    @Transactional(readOnly = true)
    public UserResponse getUserResponseById(Long userId) {
        return UserResponse.from(getUserById(userId));
    }

    @Transactional(readOnly = true)
    public UserPreferenceResponse getPreferences(Long userId) {
        User user = getUserById(userId);
        UserPreference preference = userPreferenceRepository.findByUser(user)
                .orElseGet(() -> new UserPreference(user, null, null, null));
        return UserPreferenceResponse.from(preference);
    }

    public UserPreferenceResponse updatePreferences(Long userId, UpdatePreferencesRequest request) {
        User user = getUserById(userId);
        UserPreference preference = userPreferenceRepository.findByUser(user)
                .orElseGet(() -> {
                    UserPreference p = new UserPreference();
                    p.setUser(user);
                    return p;
                });

        preference.setFavoriteGenre(request.getFavoriteGenre() != null ? request.getFavoriteGenre().trim() : null);
        preference.setFavoriteAuthor(request.getFavoriteAuthor() != null ? request.getFavoriteAuthor().trim() : null);
        preference.setFavoriteBook(request.getFavoriteBook() != null ? request.getFavoriteBook().trim() : null);

        UserPreference saved = userPreferenceRepository.save(preference);
        return UserPreferenceResponse.from(saved);
    }

    public void updatePreferences(Long userId, String favoriteGenre, String favoriteAuthor) {
        updatePreferences(userId, new UpdatePreferencesRequest(favoriteGenre, favoriteAuthor, null));
    }

    @Transactional(readOnly = true)
    public List<BookResponse> getBooksOwnedByUser(Long userId) {
        User user = getUserById(userId);
        return bookRepository.findByOwner(user).stream()
                .map(BookResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getBorrowersOfUserBooks(Long userId) {
        User owner = getUserById(userId);
        return borrowingRecordRepository.findByOwner(owner).stream()
                .map(BorrowingRecord::getBorrower)
                .distinct()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookResponse> getBooksBorrowedByUser(Long userId) {
        User borrower = getUserById(userId);
        return borrowingRecordRepository.findByBorrower(borrower).stream()
                .map(BorrowingRecord::getBook)
                .map(BookResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Notification> getNotifications(Long userId) {
        User user = getUserById(userId);
        return notificationRepository.findByUserOrderByDueDateAsc(user);
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getInboxMessages(Long userId) {
        getUserById(userId); // validate user exists
        return messageRepository.findByRecipientIdOrderByCreatedDateDesc(userId).stream()
                .map(MessageResponse::from)
                .toList();
    }

    public void sendReturnReminder(Long borrowerId, Long bookId) {
        User borrower = getUserById(borrowerId);
        String message = "Return your borrowed book by the due date";
        Notification notification = new Notification(borrower, message, LocalDate.now().plusDays(7));
        notificationRepository.save(notification);
    }
}
