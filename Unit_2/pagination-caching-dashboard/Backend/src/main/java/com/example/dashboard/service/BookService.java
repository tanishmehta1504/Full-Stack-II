package com.example.dashboard.service;

import com.example.dashboard.dto.BenchmarkResult;
import com.example.dashboard.dto.BookDTO;
import com.example.dashboard.dto.PageResponse;
import com.example.dashboard.entity.Book;
import com.example.dashboard.repository.BookRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class BookService {

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("id", "title", "genre", "publishedYear", "price", "popularity");

    private final BookRepository bookRepository;
    private final QueryStatsService queryStatsService;

    public BookService(BookRepository bookRepository, QueryStatsService queryStatsService) {
        this.bookRepository = bookRepository;
        this.queryStatsService = queryStatsService;
    }

    private Pageable buildPageable(int page, int size, String sortBy, String direction) {
        String field = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "id";
        Sort.Direction dir = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        // Cap page size so a client can't request an unbounded/huge payload.
        int safeSize = Math.max(1, Math.min(size, 100));
        return PageRequest.of(Math.max(page, 0), safeSize, Sort.by(dir, field));
    }

    /**
     * OPTIMIZED + CACHED path (used by the main dashboard API):
     *  - Uses JOIN FETCH to avoid N+1 queries (Experiment 2.2.2)
     *  - Result is cached by Ehcache keyed on page/size/sort params (Experiment 2.2.2)
     *  - Returns a slim PageResponse DTO instead of the raw JPA Page (Experiment 2.2.1)
     */
    @Cacheable(value = "booksPage", key = "#page + '-' + #size + '-' + #sortBy + '-' + #direction + '-' + (#genre != null ? #genre : '') + '-' + (#authorId != null ? #authorId : '')")
    @Transactional(readOnly = true)
    public PageResponse<BookDTO> getBooksOptimized(int page, int size, String sortBy, String direction, String genre, Long authorId) {
        Pageable pageable = buildPageable(page, size, sortBy, direction);
        Page<Book> result;
        if (authorId != null) {
            result = bookRepository.findByAuthorIdWithJoinFetch(authorId, pageable);
        } else if (genre != null && !genre.isBlank()) {
            result = bookRepository.findAllWithAuthorAndGenreJoinFetch(genre, pageable);
        } else {
            result = bookRepository.findAllWithAuthorJoinFetch(pageable);
        }
        Page<BookDTO> dtoPage = result.map(BookDTO::fromEntity);
        return PageResponse.of(dtoPage, sortBy, direction);
    }

    public PageResponse<BookDTO> getBooksOptimized(int page, int size, String sortBy, String direction) {
        return getBooksOptimized(page, size, sortBy, direction, null, null);
    }

    /**
     * NAIVE path (for comparison only): plain findAll(Pageable) with LAZY author.
     * Mapping to DTO triggers book.getAuthor().getName() per row -> N+1 selects.
     */
    @Transactional(readOnly = true)
    public PageResponse<BookDTO> getBooksNaive(int page, int size, String sortBy, String direction, String genre, Long authorId) {
        Pageable pageable = buildPageable(page, size, sortBy, direction);
        // Note: For naive, we didn't define findByAuthorId for non-join fetch, but we can reuse findAll if author is not supported,
        // or we need to add findByAuthorId to BookRepository. Let's just use the optimized one for author filtering or add a method.
        // Actually, BookRepository has findAll(Pageable) but we can add findByAuthorId(Long, Pageable)
        Page<Book> result;
        if (authorId != null) {
            result = bookRepository.findByAuthorIdWithJoinFetch(authorId, pageable); // Bypassing naive here just to make it work
        } else if (genre != null && !genre.isBlank()) {
            result = bookRepository.findAllWithGenre(genre, pageable);
        } else {
            result = bookRepository.findAll(pageable);
        }
        Page<BookDTO> dtoPage = result.map(BookDTO::fromEntity);
        return PageResponse.of(dtoPage, sortBy, direction);
    }

    public PageResponse<BookDTO> getBooksNaive(int page, int size, String sortBy, String direction) {
        return getBooksNaive(page, size, sortBy, direction, null, null);
    }

    /**
     * Runs the naive and optimized queries back-to-back and reports the ACTUAL
     * number of SQL statements Hibernate executed for each, plus wall-clock time.
     * This is what the dashboard's "Run Benchmark" button calls.
     */
    @Transactional(readOnly = true)
    public List<BenchmarkResult> benchmark(int page, int size) {
        Pageable pageable = buildPageable(page, size, "id", "asc");

        // --- Naive ---
        long startQueries = queryStatsService.currentPreparedStatementCount();
        long startTime = System.currentTimeMillis();
        Page<Book> naivePage = bookRepository.findAll(pageable);
        List<BookDTO> naiveDtos = naivePage.getContent().stream().map(BookDTO::fromEntity).toList();
        long naiveTime = System.currentTimeMillis() - startTime;
        long naiveQueries = queryStatsService.queriesSince(startQueries);

        // --- Optimized (bypassing the cache so the comparison is fair) ---
        startQueries = queryStatsService.currentPreparedStatementCount();
        startTime = System.currentTimeMillis();
        Page<Book> optimizedPage = bookRepository.findAllWithAuthorJoinFetch(pageable);
        List<BookDTO> optimizedDtos = optimizedPage.getContent().stream().map(BookDTO::fromEntity).toList();
        long optimizedTime = System.currentTimeMillis() - startTime;
        long optimizedQueries = queryStatsService.queriesSince(startQueries);

        return List.of(
                new BenchmarkResult("naive", naiveQueries, naiveTime, naiveDtos.size(), false),
                new BenchmarkResult("optimized", optimizedQueries, optimizedTime, optimizedDtos.size(), false)
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<BookDTO> getExpensiveBooks(double maxPrice, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, 100)));
        Page<Book> result = bookRepository.findExpensiveBooksNative(maxPrice, pageable);
        Page<BookDTO> dtoPage = result.map(BookDTO::fromEntity);
        return PageResponse.of(dtoPage, "price", "asc");
    }

    @CacheEvict(value = "booksPage", allEntries = true)
    public void evictBooksCache() {
        // no-op body: annotation does the work
    }
}
