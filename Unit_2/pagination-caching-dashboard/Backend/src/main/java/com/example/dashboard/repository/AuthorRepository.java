package com.example.dashboard.repository;

import com.example.dashboard.entity.Author;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorRepository extends JpaRepository<Author, Long> {
    // Spring Data derives the paginated + sorted query automatically from Pageable.
    Page<Author> findAll(Pageable pageable);
}
