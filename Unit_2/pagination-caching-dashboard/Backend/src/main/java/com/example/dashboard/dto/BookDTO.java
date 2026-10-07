package com.example.dashboard.dto;

import com.example.dashboard.entity.Book;
import java.io.Serializable;

public class BookDTO implements Serializable {
    private Long id;
    private String title;
    private String genre;
    private Integer publishedYear;
    private Double price;
    private Double popularity;
    private String authorName;
    private String authorCountry;

    public BookDTO() {}

    public static BookDTO fromEntity(Book book) {
        BookDTO dto = new BookDTO();
        dto.id = book.getId();
        dto.title = book.getTitle();
        dto.genre = book.getGenre();
        dto.publishedYear = book.getPublishedYear();
        dto.price = book.getPrice();
        dto.popularity = book.getPopularity();
        // These two calls are what trigger the lazy-load per row in the "naive" path
        if (book.getAuthor() != null) {
            dto.authorName = book.getAuthor().getName();
            dto.authorCountry = book.getAuthor().getCountry();
        }
        return dto;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getGenre() { return genre; }
    public Integer getPublishedYear() { return publishedYear; }
    public Double getPrice() { return price; }
    public Double getPopularity() { return popularity; }
    public String getAuthorName() { return authorName; }
    public String getAuthorCountry() { return authorCountry; }
}
