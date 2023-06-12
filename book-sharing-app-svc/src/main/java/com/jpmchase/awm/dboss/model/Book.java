package com.jpmchase.awm.dboss.model;

import lombok.Data;

import java.util.List;

@Data
public class Book {
    private String bookId;
    private String name;
    private String author;
    private String owner;
    private Double rating;
    private Integer totalAvailability;
    private List<String> waitList;
}
