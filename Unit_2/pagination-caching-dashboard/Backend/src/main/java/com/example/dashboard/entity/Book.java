package com.example.dashboard.entity;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "books")
public class Book implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String genre;

    private Integer publishedYear;

    private Double price;

    private Double popularity;

    // LAZY on purpose: accessing author.getName() per-row without JOIN FETCH
    // is exactly what causes the classic N+1 query problem (Experiment 2.2.2).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private Author author;

    public Book() {}

    public Book(String title, String genre, Integer publishedYear, Double price, Double popularity, Author author) {
        this.title = title;
        this.genre = genre;
        this.publishedYear = publishedYear;
        this.price = price;
        this.popularity = popularity;
        this.author = author;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
    public Integer getPublishedYear() { return publishedYear; }
    public void setPublishedYear(Integer publishedYear) { this.publishedYear = publishedYear; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public Double getPopularity() { return popularity; }
    public void setPopularity(Double popularity) { this.popularity = popularity; }
    public Author getAuthor() { return author; }
    public void setAuthor(Author author) { this.author = author; }
}
