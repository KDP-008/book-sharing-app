package com.kdp.app.service;

import com.kdp.app.dto.CartItemResponse;
import com.kdp.app.dto.CheckoutResponse;
import com.kdp.app.model.*;
import com.kdp.app.repository.BookRepository;
import com.kdp.app.repository.CartItemRepository;
import com.kdp.app.repository.BorrowingRecordRepository;
import com.kdp.app.repository.MessageRepository;
import com.kdp.app.repository.NotificationRepository;
import com.kdp.app.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final BorrowingRecordRepository borrowingRecordRepository;
    private final NotificationRepository notificationRepository;
    private final MessageRepository messageRepository;

    public CartService(CartItemRepository cartItemRepository,
                       UserRepository userRepository,
                       BookRepository bookRepository,
                       BorrowingRecordRepository borrowingRecordRepository,
                       NotificationRepository notificationRepository,
                       MessageRepository messageRepository) {
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.borrowingRecordRepository = borrowingRecordRepository;
        this.notificationRepository = notificationRepository;
        this.messageRepository = messageRepository;
    }

    public CartItemResponse addToCart(Long userId, Long bookId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required.");
        }
        if (bookId == null) {
            throw new IllegalArgumentException("Book ID is required.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with id: " + bookId));

        if (book.getOwner() != null && book.getOwner().getId().equals(userId)) {
            throw new IllegalArgumentException("Cannot borrow your own book.");
        }

        if (cartItemRepository.existsByUserAndBook(user, book)) {
            throw new IllegalArgumentException("Book is already in your cart.");
        }

        if (!book.isAvailable()) {
            throw new IllegalStateException("Book is not available for checkout");
        }

        CartItem item = new CartItem(user, book);
        CartItem saved = cartItemRepository.save(item);
        return CartItemResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<CartItemResponse> getCartItems(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        return cartItemRepository.findByUser(user).stream()
                .map(CartItemResponse::from)
                .toList();
    }

    public void removeCartItem(Long userId, Long cartItemId) {
        CartItem item = cartItemRepository.findByIdAndUserId(cartItemId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found for user: " + cartItemId));
        cartItemRepository.delete(item);
    }

    public CheckoutResponse checkout(Long userId, String deliveryMethod) {
        User borrower = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        List<CartItem> items = cartItemRepository.findByUser(borrower);
        if (items.isEmpty()) {
            throw new IllegalStateException("Cart is empty.");
        }

        String methodStr = (deliveryMethod == null || deliveryMethod.isBlank()) ? "COURIER" : deliveryMethod.trim();
        DeliveryMethod method = DeliveryMethod.valueOf(methodStr.toUpperCase());
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

            Message message = new Message(
                    borrower,
                    "Book Sharing App",
                    "You borrowed '" + book.getTitle() + "'. Due date: " + dueDate,
                    dueDate
            );
            messageRepository.save(message);
        }

        cartItemRepository.deleteByUser(borrower);

        return new CheckoutResponse("Checkout successful", method.name(), records.size());
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
