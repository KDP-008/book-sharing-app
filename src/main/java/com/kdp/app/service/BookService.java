package com.kdp.app.service;

import com.kdp.app.dto.BookResponse;
import com.kdp.app.dto.CreateBookRequest;
import com.kdp.app.model.Book;
import com.kdp.app.model.BorrowingRecord;
import com.kdp.app.model.User;
import com.kdp.app.repository.BookRepository;
import com.kdp.app.repository.BorrowingRecordRepository;
import com.kdp.app.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class BookService {

    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final BorrowingRecordRepository borrowingRecordRepository;

    public BookService(BookRepository bookRepository,
                       UserRepository userRepository,
                       BorrowingRecordRepository borrowingRecordRepository) {
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.borrowingRecordRepository = borrowingRecordRepository;
    }

    public BookResponse addBook(CreateBookRequest request) {
        if (request.getOwnerId() == null) {
            throw new IllegalArgumentException("Owner ID is required.");
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("Title is required.");
        }
        if (request.getAuthor() == null || request.getAuthor().isBlank()) {
            throw new IllegalArgumentException("Author is required.");
        }
        if (request.getGenre() == null || request.getGenre().isBlank()) {
            throw new IllegalArgumentException("Genre is required.");
        }

        User owner = userRepository.findById(request.getOwnerId())
                .orElseThrow(() -> new IllegalArgumentException("Owner not found with id: " + request.getOwnerId()));

        Book book = new Book(request.getTitle().trim(), request.getAuthor().trim(), request.getGenre().trim(), request.isAvailable(), owner);
        owner.getOwnedBooks().add(book);
        Book saved = bookRepository.save(book);
        return BookResponse.from(saved);
    }

    public Book addBook(Long ownerId, String title, String author, String genre, boolean available) {
        CreateBookRequest req = new CreateBookRequest();
        req.setOwnerId(ownerId);
        req.setTitle(title);
        req.setAuthor(author);
        req.setGenre(genre);
        req.setAvailable(available);
        addBook(req);
        return bookRepository.findByOwner(userRepository.findById(ownerId).orElseThrow()).stream()
                .filter(b -> b.getTitle().equals(title))
                .findFirst()
                .orElseThrow();
    }

    @Transactional(readOnly = true)
    public List<BookResponse> searchBooks(String title, String author, String genre) {
        String titleValue = normalize(title);
        String authorValue = normalize(author);
        String genreValue = normalize(genre);

        return bookRepository.searchBooks(titleValue, authorValue, genreValue).stream()
                .map(BookResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Book getBookById(Long bookId) {
        return bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with id: " + bookId));
    }

    @Transactional(readOnly = true)
    public BookResponse getBookResponseById(Long bookId) {
        return BookResponse.from(getBookById(bookId));
    }

    @Transactional(readOnly = true)
    public List<BookResponse> getAllBooks() {
        return bookRepository.findAll().stream()
                .map(BookResponse::from)
                .toList();
    }

    public BookResponse returnBook(Long bookId, Long borrowerId) {
        if (borrowerId == null) {
            throw new IllegalArgumentException("Borrower ID is required.");
        }

        Book book = getBookById(bookId);

        BorrowingRecord record = borrowingRecordRepository.findByBookIdAndBorrowerIdAndActiveTrue(bookId, borrowerId)
                .orElseThrow(() -> new IllegalArgumentException("No active borrowing record found for book ID " + bookId + " and borrower ID " + borrowerId));

        book.setAvailable(true);
        bookRepository.save(book);

        record.setActive(false);
        record.setReturnDate(LocalDate.now());
        borrowingRecordRepository.save(record);

        return BookResponse.from(book);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
