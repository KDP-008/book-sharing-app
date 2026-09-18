package com.kdp.app.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "favorite_genre")
    private String favoriteGenre;

    @Column(name = "favorite_author")
    private String favoriteAuthor;

    @Column(name = "favorite_book")
    private String favoriteBook;

    public UserPreference(User user, String favoriteGenre, String favoriteAuthor, String favoriteBook) {
        this.user = user;
        this.favoriteGenre = favoriteGenre;
        this.favoriteAuthor = favoriteAuthor;
        this.favoriteBook = favoriteBook;
    }
}

