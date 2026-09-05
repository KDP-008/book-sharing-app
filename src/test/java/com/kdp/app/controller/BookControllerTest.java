package com.kdp.app.controller;

import com.kdp.app.model.Book;
import com.kdp.app.model.User;
import com.kdp.app.service.BookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BookControllerTest {

    private MockMvc mockMvc;
    private BookService bookService;

    @BeforeEach
    void setUp() {
        bookService = mock(BookService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new BookController(bookService)).build();
    }

    @Test
    void searchBooks_shouldReturnMatchingBooks() throws Exception {
        User owner = new User("Owner", "owner@example.com", "pass");
        owner.setId(1L);
        Book book = new Book("The Pragmatic Programmer", "Andrew Hunt", "Programming", true, owner);
        book.setId(10L);
        when(bookService.searchBooks("Pragmatic", null, null)).thenReturn(List.of(book));

        mockMvc.perform(get("/books/search").param("title", "Pragmatic"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("The Pragmatic Programmer"));
    }

    @Test
    void addBook_shouldPersistThroughApi() throws Exception {
        User owner = new User("Owner", "owner@example.com", "pass");
        owner.setId(1L);
        Book book = new Book("Clean Code", "Robert C. Martin", "Programming", true, owner);
        book.setId(20L);

        when(bookService.addBook(1L, "Clean Code", "Robert C. Martin", "Programming", true)).thenReturn(book);

        mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ownerId\":1,\"title\":\"Clean Code\",\"author\":\"Robert C. Martin\",\"genre\":\"Programming\",\"available\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Clean Code"));
    }
}
