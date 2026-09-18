package com.kdp.app.controller;

import com.kdp.app.dto.BookResponse;
import com.kdp.app.dto.CreateBookRequest;
import com.kdp.app.service.BookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
        BookResponse book = new BookResponse(10L, "The Pragmatic Programmer", "Andrew Hunt", "Programming", true, 1L, "Owner");
        when(bookService.searchBooks("Pragmatic", null, null)).thenReturn(List.of(book));

        mockMvc.perform(get("/books/search").param("title", "Pragmatic"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("The Pragmatic Programmer"));
    }

    @Test
    void addBook_shouldPersistThroughApi() throws Exception {
        BookResponse book = new BookResponse(20L, "Clean Code", "Robert C. Martin", "Programming", true, 1L, "Owner");
        when(bookService.addBook(any(CreateBookRequest.class))).thenReturn(book);

        mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ownerId\":1,\"title\":\"Clean Code\",\"author\":\"Robert C. Martin\",\"genre\":\"Programming\",\"available\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Clean Code"));
    }

    @Test
    void returnBook_shouldWorkThroughApi() throws Exception {
        BookResponse book = new BookResponse(20L, "Clean Code", "Robert C. Martin", "Programming", true, 1L, "Owner");
        when(bookService.returnBook(eq(20L), eq(2L))).thenReturn(book);

        mockMvc.perform(post("/books/20/return")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"borrowerId\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));
    }
}
