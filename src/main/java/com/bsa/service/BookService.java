package com.bsa.service;

import com.bsa.model.Book;
import com.bsa.model.BorrowingRecord;
import com.bsa.model.User;
import com.bsa.repository.BookRepository;
import com.bsa.repository.BorrowingRecordRepository;
import com.bsa.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final BorrowingRecordRepository borrowingRecordRepository;
    private final ReservationService reservationService;

    public BookService(BookRepository bookRepository, UserRepository userRepository,
                      BorrowingRecordRepository borrowingRecordRepository,
                      ReservationService reservationService) {
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.borrowingRecordRepository = borrowingRecordRepository;
        this.reservationService = reservationService;
    }

    @Transactional
    public Book addBook(Long ownerId, String title, String author, String genre, boolean available) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Owner not found with id: " + ownerId));

        Book book = new Book(title, author, genre, available, owner);
        owner.getOwnedBooks().add(book);
        return bookRepository.save(book);
    }

    public List<Book> searchBooks(String title, String author, String genre) {
        String titleValue = normalize(title);
        String authorValue = normalize(author);
        String genreValue = normalize(genre);

        return bookRepository.searchBooks(titleValue, authorValue, genreValue);
    }

    public Book getBookById(Long bookId) {
        return bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with id: " + bookId));
    }

    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    @Transactional
    public BorrowingRecord returnBook(Long bookId, Long borrowerId) {
        // Lock the book row up front: returning it may trigger queue promotion, which must
        // be serialized against concurrent claims/cancellations/other returns for this book.
        Book book = bookRepository.findByIdForUpdate(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with id: " + bookId));
        User borrower = userRepository.findById(borrowerId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + borrowerId));

        BorrowingRecord record = borrowingRecordRepository.findByBookAndBorrowerAndActiveTrue(book, borrower)
                .orElseThrow(() -> new IllegalStateException(
                        "No active borrowing record found for this book and user."));

        record.setReturnDate(LocalDate.now());
        record.setActive(false);
        borrowingRecordRepository.save(record);

        reservationService.handleBookReturned(book);

        return record;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
