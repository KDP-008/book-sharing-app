package com.kdp.app.dto;

import com.kdp.app.model.Message;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Inbox message details")
public class MessageResponse {

    private Long id;
    private Long recipientId;
    private String sender;
    private String subject;
    private LocalDate actionDueDate;
    private LocalDateTime createdDate;
    private boolean isRead;

    public static MessageResponse from(Message message) {
        if (message == null) {
            return null;
        }
        Long rId = message.getRecipient() != null ? message.getRecipient().getId() : null;
        return new MessageResponse(
                message.getId(),
                rId,
                message.getSender(),
                message.getSubject(),
                message.getActionDueDate(),
                message.getCreatedDate(),
                message.isRead()
        );
    }
}

