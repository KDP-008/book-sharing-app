package com.jpmchase.awm.dboss.controller;

import com.jpmchase.awm.dboss.model.Book;
import com.jpmchase.awm.dboss.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/book")
@RequiredArgsConstructor
@Slf4j
public class BookLibraryController {

    private final BookRepository bookRepository;

    @GetMapping("/v1/getAllBooks")
    public List<Book> getAllBookDetails() {
        return bookRepository.getAllBooks();
    }

    @GetMapping("/v1/getBook/{bookName}")
    public Book bookDetails(@PathVariable String bookName) {
        log.info("Getting details of book: {}", bookName);
        return bookRepository.getBookDetailsByName(bookName);
    }
}
