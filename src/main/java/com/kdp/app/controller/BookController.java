package com.kdp.app.controller;

import com.kdp.app.dto.BookResponse;
import com.kdp.app.dto.CreateBookRequest;
import com.kdp.app.dto.ReturnBookRequest;
import com.kdp.app.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<List<BookResponse>> getAllBooks() {
        return ResponseEntity.ok(bookService.getAllBooks());
    }

    @GetMapping("/search")
    @Operation(summary = "Search books by title, author or genre")
    public ResponseEntity<List<BookResponse>> searchBooks(@RequestParam(required = false) String title,
                                                          @RequestParam(required = false) String author,
                                                          @RequestParam(required = false) String genre) {
        return ResponseEntity.ok(bookService.searchBooks(title, author, genre));
    }

    @GetMapping("/{bookId}")
    @Operation(summary = "Get book details by id")
    public ResponseEntity<BookResponse> getBook(@PathVariable Long bookId) {
        return ResponseEntity.ok(bookService.getBookResponseById(bookId));
    }

    @PostMapping
    @Operation(summary = "Add a new book")
    public ResponseEntity<BookResponse> addBook(@RequestBody CreateBookRequest request) {
        BookResponse book = bookService.addBook(request);
        return ResponseEntity.ok(book);
    }

    @PostMapping("/{bookId}/return")
    @Operation(summary = "Return a borrowed book")
    public ResponseEntity<BookResponse> returnBook(@PathVariable Long bookId,
                                                   @RequestBody(required = false) ReturnBookRequest request,
                                                   @RequestParam(required = false) Long borrowerId) {
        Long effectiveBorrowerId = borrowerId;
        if (effectiveBorrowerId == null && request != null) {
            effectiveBorrowerId = request.getBorrowerId();
        }
        if (effectiveBorrowerId == null) {
            throw new IllegalArgumentException("Borrower ID is required to return a book.");
        }

        BookResponse returnedBook = bookService.returnBook(bookId, effectiveBorrowerId);
        return ResponseEntity.ok(returnedBook);
    }
}
