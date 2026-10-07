package com.example.dashboard.controller;

import com.example.dashboard.dto.BenchmarkResult;
import com.example.dashboard.dto.BookDTO;
import com.example.dashboard.dto.PageResponse;
import com.example.dashboard.service.BookService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books")
@CrossOrigin(origins = "*")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    /**
     * Main dashboard endpoint: paginated + sortable + JOIN FETCH-optimized + cached.
     * Example: GET /api/books?page=0&size=10&sortBy=title&direction=asc
     */
    @GetMapping
    public PageResponse<BookDTO> getBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) Long authorId) {
        return bookService.getBooksOptimized(page, size, sortBy, direction, genre, authorId);
    }

    /**
     * Same data, but via the naive (non-JOIN-FETCH, uncached) path.
     * Kept around purely so the dashboard can demonstrate the difference.
     */
    @GetMapping("/naive")
    public PageResponse<BookDTO> getBooksNaive(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) Long authorId) {
        return bookService.getBooksNaive(page, size, sortBy, direction, genre, authorId);
    }

    /** Runs naive vs optimized back-to-back and returns real Hibernate query counts + timings. */
    @GetMapping("/benchmark")
    public List<BenchmarkResult> benchmark(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return bookService.benchmark(page, size);
    }

    /** Native SQL query example (Experiment 2.2.2). */
    @GetMapping("/expensive")
    public PageResponse<BookDTO> getExpensiveBooks(
            @RequestParam(defaultValue = "50", required = false) Double maxPrice,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        double threshold = maxPrice != null ? maxPrice : (minPrice != null ? minPrice : 50.0);
        return bookService.getExpensiveBooks(threshold, page, size);
    }
}
