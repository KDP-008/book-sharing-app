package com.kdp.app.service;
import org.springframework.stereotype.Service;
@Service
public class SmsNotificationService {
    public void sendOverdueSmsNotice(String phoneNumber, String bookTitle) {
        // SMS integration for automated overdue notices
        System.out.println("Sending SMS to " + phoneNumber + " for overdue book: " + bookTitle);
    }
}
