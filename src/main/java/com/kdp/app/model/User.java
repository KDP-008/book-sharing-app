package com.kdp.app.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Schema(description = "A user of the Book Sharing application")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Schema(description = "User's full name", example = "Alice Smith")
    private String name;

    @Column(nullable = false, unique = true)
    @Schema(description = "User's email address", example = "alice@example.com")
    private String email;

    @Column(nullable = false)
    @Schema(description = "Hashed password (never return raw password)")
    private String password;

    @Column(name = "memorable_info", nullable = false)
    @Schema(description = "Memorable info used for password reset verification")
    private String memorableInfo;

    @Column(name = "is_locked", nullable = false, length = 1)
    private String isLocked = "N";

    @Column(name = "is_active", nullable = false, length = 1)
    private String isActive = "Y";

    @Column(name = "create_date", nullable = false)
    private LocalDateTime createDate = LocalDateTime.now();

    @Column(name = "modified_date", nullable = false)
    private LocalDateTime modifiedDate = LocalDateTime.now();

    @JsonIgnore
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private UserPreference preference;

    @JsonIgnore
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Book> ownedBooks = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "borrower", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BorrowingRecord> borrowedRecords = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Notification> notifications = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> cartItems = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "recipient", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Message> messages = new ArrayList<>();

    public User(String name, String email, String password, String memorableInfo) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.memorableInfo = memorableInfo;
        this.isLocked = "N";
        this.isActive = "Y";
        this.createDate = LocalDateTime.now();
        this.modifiedDate = LocalDateTime.now();
    }

    public User(String name, String email, String password) {
        this(name, email, password, "default-memorable-info");
    }
}
