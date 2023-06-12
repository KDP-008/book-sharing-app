package com.jpmchase.awm.dboss.repository;

import com.jpmchase.awm.dboss.model.Book;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class BookRepository {

    public Book getBookDetailsByName(String bookName) {
        return getDummyBooks().stream()
                .filter(books -> books.getName().equals(bookName))
                .findAny().
                orElse(null);
    }

    public List<Book> getAllBooks() {
        return getDummyBooks();
    }

    private List<Book> getDummyBooks() {
        Book book = new Book();
        book.setBookId("book1");
        book.setName("The Theory of Everything: The Origin and Fate of the Universe");
        book.setAuthor("Stephen Hawking");
        book.setOwner("Devanand");
        book.setRating(4.6);
        book.setTotalAvailability(0);
        book.setWaitList(List.of("Kamal", "Deep"));

        Book book1 = new Book();
        book.setBookId("book2");
        book1.setName("A Brief History Of Time: From Big Bang To Black Holes");
        book1.setAuthor("Stephen Hawking");
        book1.setOwner("Elliot");
        book1.setRating(4.6);
        book1.setTotalAvailability(5);
        book1.setWaitList(List.of());

        return List.of(book, book1);
    }

}
