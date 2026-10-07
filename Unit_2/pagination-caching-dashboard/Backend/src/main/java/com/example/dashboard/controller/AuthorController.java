package com.example.dashboard.controller;

import com.example.dashboard.dto.AuthorDTO;
import com.example.dashboard.dto.PageResponse;
import com.example.dashboard.service.AuthorService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/authors")
@CrossOrigin(origins = "*")
public class AuthorController {

    private final AuthorService authorService;

    public AuthorController(AuthorService authorService) {
        this.authorService = authorService;
    }

    @GetMapping
    public PageResponse<AuthorDTO> getAuthors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        return authorService.getAuthors(page, size, sortBy, direction);
    }
}
