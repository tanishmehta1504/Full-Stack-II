package com.example.dashboard.repository;

import com.example.dashboard.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, Long> {

    /**
     * NAIVE: inherited findAll(Pageable). Author is FetchType.LAZY, so if the
     * caller touches book.getAuthor().getName() for every row (as our DTO
     * mapper does), Hibernate fires ONE extra SELECT per row -> the classic
     * N+1 query problem (Experiment 2.2.2).
     */
    Page<Book> findAll(Pageable pageable);

    /**
     * OPTIMIZED: JOIN FETCH pulls the related Author in the SAME query,
     * eliminating the N+1 problem. A separate countQuery is required because
     * JOIN FETCH cannot be used directly inside a COUNT(...) query.
     */
    @Query(value = "SELECT b FROM Book b JOIN FETCH b.author a",
           countQuery = "SELECT COUNT(b) FROM Book b")
    Page<Book> findAllWithAuthorJoinFetch(Pageable pageable);

    @Query(value = "SELECT b FROM Book b JOIN FETCH b.author a WHERE (:genre IS NULL OR :genre = '' OR LOWER(b.genre) LIKE LOWER(CONCAT('%', :genre, '%')))",
           countQuery = "SELECT COUNT(b) FROM Book b WHERE (:genre IS NULL OR :genre = '' OR LOWER(b.genre) LIKE LOWER(CONCAT('%', :genre, '%')))")
    Page<Book> findAllWithAuthorAndGenreJoinFetch(@Param("genre") String genre, Pageable pageable);

    @Query(value = "SELECT b FROM Book b WHERE (:genre IS NULL OR :genre = '' OR LOWER(b.genre) LIKE LOWER(CONCAT('%', :genre, '%')))")
    Page<Book> findAllWithGenre(@Param("genre") String genre, Pageable pageable);

    @Query(value = "SELECT b FROM Book b JOIN FETCH b.author a WHERE a.id = :authorId",
           countQuery = "SELECT COUNT(b) FROM Book b WHERE b.author.id = :authorId")
    Page<Book> findByAuthorIdWithJoinFetch(@Param("authorId") Long authorId, Pageable pageable);

    /**
     * Native SQL example (Experiment 2.2.2: "native queries for complex operations").
     * Returns books priced up to a maximum price threshold, joined with author name, paginated in ascending order.
     */
    @Query(value = "SELECT b.* FROM books b JOIN authors a ON b.author_id = a.id " +
                   "WHERE b.price <= :maxPrice ORDER BY b.price ASC",
           countQuery = "SELECT COUNT(*) FROM books b WHERE b.price <= :maxPrice",
           nativeQuery = true)
    Page<Book> findExpensiveBooksNative(@Param("maxPrice") double maxPrice, Pageable pageable);
}
