package com.kdp.app.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Column(nullable = false, length = 300)
    private String sender;

    @Column(nullable = false, length = 2000)
    private String subject;

    @Column(name = "action_due_date")
    private LocalDate actionDueDate;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate = LocalDateTime.now();

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    public Message(User recipient, String sender, String subject, LocalDate actionDueDate) {
        this.recipient = recipient;
        this.sender = sender;
        this.subject = subject;
        this.actionDueDate = actionDueDate;
        this.createdDate = LocalDateTime.now();
        this.isRead = false;
    }
}

