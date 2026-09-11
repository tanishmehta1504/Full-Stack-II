package com.labs.postservice.controller;

import com.labs.postservice.dto.ApiResponse;
import com.labs.postservice.dto.PostRequest;
import com.labs.postservice.dto.PostResponse;
import com.labs.postservice.service.PostService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PostResponse>> create(@Valid @RequestBody PostRequest request) {
        PostResponse created = postService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "post scheduled"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PostResponse>> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(postService.getById(id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PostResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(postService.getAll()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PostResponse>> update(@PathVariable Long id,
                                                              @Valid @RequestBody PostRequest request) {
        return ResponseEntity.ok(ApiResponse.success(postService.update(id, request), "post updated"));
    }

    @PatchMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<PostResponse>> publish(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(postService.publishNow(id), "post published"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        postService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "post deleted"));
    }
}
