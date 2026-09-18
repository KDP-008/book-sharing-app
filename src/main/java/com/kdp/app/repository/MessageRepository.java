package com.kdp.app.repository;

import com.kdp.app.model.Message;
import com.kdp.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByRecipientOrderByCreatedDateDesc(User recipient);
    List<Message> findByRecipientIdOrderByCreatedDateDesc(Long recipientId);
}

