package com.example.dashboard.service;

import com.example.dashboard.dto.AuthorDTO;
import com.example.dashboard.dto.PageResponse;
import com.example.dashboard.entity.Author;
import com.example.dashboard.repository.AuthorRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class AuthorService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "name", "country");

    private final AuthorRepository authorRepository;

    public AuthorService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    @Cacheable(value = "authorsPage", key = "#page + '-' + #size + '-' + #sortBy + '-' + #direction")
    @Transactional(readOnly = true)
    public PageResponse<AuthorDTO> getAuthors(int page, int size, String sortBy, String direction) {
        String field = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "id";
        Sort.Direction dir = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        int safeSize = Math.max(1, Math.min(size, 100));
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize, Sort.by(dir, field));

        Page<Author> result = authorRepository.findAll(pageable);
        Page<AuthorDTO> dtoPage = result.map(AuthorDTO::fromEntity);
        return PageResponse.of(dtoPage, sortBy, direction);
    }

    @CacheEvict(value = "authorsPage", allEntries = true)
    public void evictAuthorsCache() {
    }
}
