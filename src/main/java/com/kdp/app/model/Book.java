package com.kdp.app.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "books")
@Schema(description = "A book available for sharing")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Schema(description = "Title of the book", example = "The Hobbit")
    private String title;

    @Column(nullable = false)
    @Schema(description = "Author of the book", example = "J.R.R. Tolkien")
    private String author;

    @Column(nullable = false)
    @Schema(description = "Genre of the book", example = "Fantasy")
    private String genre;

    @Column(nullable = false)
    @Schema(description = "Availability for borrowing")
    private boolean available = true;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @JsonIgnore
    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BorrowingRecord> borrowingRecords = new ArrayList<>();

    public Book(String title, String author, String genre, boolean available, User owner) {
        this.title = title;
        this.author = author;
        this.genre = genre;
        this.available = available;
        this.owner = owner;
    }
}
