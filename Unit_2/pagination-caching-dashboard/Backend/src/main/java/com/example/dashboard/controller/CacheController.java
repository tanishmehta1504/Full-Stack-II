package com.example.dashboard.controller;

import com.example.dashboard.service.AuthorService;
import com.example.dashboard.service.BookService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cache")
@CrossOrigin(origins = "*")
public class CacheController {

    private final BookService bookService;
    private final AuthorService authorService;

    public CacheController(BookService bookService, AuthorService authorService) {
        this.bookService = bookService;
        this.authorService = authorService;
    }

    @PostMapping("/clear")
    public Map<String, String> clearCache() {
        bookService.evictBooksCache();
        authorService.evictAuthorsCache();
        return Map.of("status", "cleared");
    }
}
