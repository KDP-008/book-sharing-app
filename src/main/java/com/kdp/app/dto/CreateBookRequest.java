package com.kdp.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request payload to create a new book")
public class CreateBookRequest {

    private Long ownerId;
    private String title;
    private String author;
    private String genre;
    private boolean available = true;

    public CreateBookRequest() {}

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}
