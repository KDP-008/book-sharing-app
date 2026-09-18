package com.kdp.app.dto;

import com.kdp.app.model.CartItem;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Cart item details")
public class CartItemResponse {

    private Long id;
    private Long bookId;
    private String bookTitle;
    private String bookAuthor;
    private String bookGenre;

    public static CartItemResponse from(CartItem item) {
        if (item == null) {
            return null;
        }
        Long bId = null;
        String bTitle = null;
        String bAuthor = null;
        String bGenre = null;
        try {
            if (item.getBook() != null) {
                bId = item.getBook().getId();
                bTitle = item.getBook().getTitle();
                bAuthor = item.getBook().getAuthor();
                bGenre = item.getBook().getGenre();
            }
        } catch (Exception ignored) {
        }
        return new CartItemResponse(item.getId(), bId, bTitle, bAuthor, bGenre);
    }
}

