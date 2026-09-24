package com.bsa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BookSharingApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(BookSharingApplication.class, args);
    }
}
