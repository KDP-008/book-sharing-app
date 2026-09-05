package com.kdp.app.service;

import com.kdp.app.model.*;
import com.kdp.app.repository.BookRepository;
import com.kdp.app.repository.CartItemRepository;
import com.kdp.app.repository.BorrowingRecordRepository;
import com.kdp.app.repository.NotificationRepository;
import com.kdp.app.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final BorrowingRecordRepository borrowingRecordRepository;
    private final NotificationRepository notificationRepository;

    public CartService(CartItemRepository cartItemRepository,
                      UserRepository userRepository,
                      BookRepository bookRepository,
                      BorrowingRecordRepository borrowingRecordRepository,
                      NotificationRepository notificationRepository) {
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.borrowingRecordRepository = borrowingRecordRepository;
        this.notificationRepository = notificationRepository;
    }

    public CartItem addToCart(Long userId, Long bookId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with id: " + bookId));

        if (!book.isAvailable()) {
            throw new IllegalStateException("Book is not available for checkout");
        }

        CartItem item = new CartItem(user, book);
        return cartItemRepository.save(item);
    }

    public List<CartItem> getCartItems(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        return cartItemRepository.findByUser(user);
    }

    @Transactional
    public CheckoutResult checkout(Long userId, String deliveryMethod) {
        User borrower = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        List<CartItem> items = cartItemRepository.findByUser(borrower);
        if (items.isEmpty()) {
            throw new IllegalStateException("Cart is empty.");
        }

        DeliveryMethod method = DeliveryMethod.valueOf(deliveryMethod.toUpperCase());
        List<BorrowingRecord> records = new java.util.ArrayList<>();

        for (CartItem item : items) {
            Book book = item.getBook();
            if (!book.isAvailable()) {
                throw new IllegalStateException("One or more books are unavailable.");
            }

            book.setAvailable(false);
            bookRepository.save(book);

            LocalDate borrowDate = LocalDate.now();
            LocalDate dueDate = borrowDate.plusDays(14);

            BorrowingRecord borrowingRecord = new BorrowingRecord(
                    book,
                    borrower,
                    book.getOwner(),
                    borrowDate,
                    dueDate,
                    method
            );
            borrowingRecordRepository.save(borrowingRecord);
            records.add(borrowingRecord);

            Notification notification = new Notification(
                    borrower,
                    "You borrowed '" + book.getTitle() + "'. Due date: " + dueDate,
                    dueDate
            );
            notificationRepository.save(notification);
        }

        cartItemRepository.deleteByUser(borrower);

        return new CheckoutResult(records, method);
    }

    public static class CheckoutResult {
        private final List<BorrowingRecord> borrowingRecords;
        private final DeliveryMethod deliveryMethod;

        public CheckoutResult(List<BorrowingRecord> borrowingRecords, DeliveryMethod deliveryMethod) {
            this.borrowingRecords = borrowingRecords;
            this.deliveryMethod = deliveryMethod;
        }

        public List<BorrowingRecord> getBorrowingRecords() {
            return borrowingRecords;
        }

        public DeliveryMethod getDeliveryMethod() {
            return deliveryMethod;
        }
    }
}
