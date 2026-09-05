package com.kdp.app.service;

import com.kdp.app.model.Book;
import com.kdp.app.model.User;
import com.kdp.app.repository.BookRepository;
import com.kdp.app.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    public BookService(BookRepository bookRepository, UserRepository userRepository) {
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
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

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
