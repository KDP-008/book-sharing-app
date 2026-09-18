package com.kdp.app.dto;

import com.kdp.app.model.Book;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Book details representation")
public class BookResponse {

    private Long id;
    private String title;
    private String author;
    private String genre;
    private boolean available;
    private Long ownerId;
    private String ownerName;

    public static BookResponse from(Book book) {
        if (book == null) {
            return null;
        }
        Long oId = null;
        String oName = null;
        try {
            if (book.getOwner() != null) {
                oId = book.getOwner().getId();
                oName = book.getOwner().getName();
            }
        } catch (Exception ignored) {
        }
        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getGenre(),
                book.isAvailable(),
                oId,
                oName
        );
    }
}

