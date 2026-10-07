package com.example.dashboard.dto;

import com.example.dashboard.entity.Author;
import java.io.Serializable;

public class AuthorDTO implements Serializable {
    private Long id;
    private String name;
    private String country;
    private int bookCount;

    public static AuthorDTO fromEntity(Author author) {
        AuthorDTO dto = new AuthorDTO();
        dto.id = author.getId();
        dto.name = author.getName();
        dto.country = author.getCountry();
        dto.bookCount = author.getBooks() != null ? author.getBooks().size() : 0;
        return dto;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCountry() { return country; }
    public int getBookCount() { return bookCount; }
}
