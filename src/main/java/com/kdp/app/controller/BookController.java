package com.kdp.app.controller;

import com.kdp.app.model.Book;
import com.kdp.app.service.BookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public ResponseEntity<List<Book>> getAllBooks() {
        return ResponseEntity.ok(bookService.getAllBooks());
    }

    @GetMapping("/search")
    public ResponseEntity<List<Book>> searchBooks(@RequestParam(required = false) String title,
                                                @RequestParam(required = false) String author,
                                                @RequestParam(required = false) String genre) {
        return ResponseEntity.ok(bookService.searchBooks(title, author, genre));
    }

    @GetMapping("/{bookId}")
    public ResponseEntity<Book> getBook(@PathVariable Long bookId) {
        return ResponseEntity.ok(bookService.getBookById(bookId));
    }

    @PostMapping
    public ResponseEntity<?> addBook(@RequestBody Map<String, Object> payload) {
        Book book = bookService.addBook(
                Long.valueOf(payload.get("ownerId").toString()),
                payload.get("title").toString(),
                payload.get("author").toString(),
                payload.get("genre").toString(),
                Boolean.parseBoolean(payload.getOrDefault("available", true).toString())
        );
        return ResponseEntity.ok(book);
    }
}
