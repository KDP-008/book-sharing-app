package com.kdp.app.controller;

import com.kdp.app.model.Book;
import com.kdp.app.service.BookService;
import com.kdp.app.dto.CreateBookRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/books")
@Tag(name = "Books", description = "Operations related to books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    @Operation(summary = "Get all books")
    public ResponseEntity<List<Book>> getAllBooks() {
        return ResponseEntity.ok(bookService.getAllBooks());
    }

    @GetMapping("/search")
    @Operation(summary = "Search books by title, author or genre")
    public ResponseEntity<List<Book>> searchBooks(@RequestParam(required = false) String title,
                                                @RequestParam(required = false) String author,
                                                @RequestParam(required = false) String genre) {
        return ResponseEntity.ok(bookService.searchBooks(title, author, genre));
    }

    @GetMapping("/{bookId}")
    @Operation(summary = "Get book details by id")
    public ResponseEntity<Book> getBook(@PathVariable Long bookId) {
        return ResponseEntity.ok(bookService.getBookById(bookId));
    }

    @PostMapping
    @Operation(summary = "Add a new book")
    public ResponseEntity<?> addBook(@RequestBody CreateBookRequest request) {
        try {
            Book book = bookService.addBook(
                    request.getOwnerId(),
                    request.getTitle(),
                    request.getAuthor(),
                    request.getGenre(),
                    request.isAvailable()
            );
            return ResponseEntity.ok(book);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }
}
