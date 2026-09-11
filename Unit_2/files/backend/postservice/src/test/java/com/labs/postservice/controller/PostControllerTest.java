package com.labs.postservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labs.postservice.dto.PostRequest;
import com.labs.postservice.dto.PostResponse;
import com.labs.postservice.entity.Post;
import com.labs.postservice.entity.PostStatus;
import com.labs.postservice.exception.PostNotFoundException;
import com.labs.postservice.service.PostService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PostController.class)
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PostService postService;

    @Test
    @DisplayName("POST /api/posts - Create post valid payload returns 201 CREATED")
    void testCreatePost_Success() throws Exception {
        PostRequest req = new PostRequest("New Release Announcement", "We are launching next week!", LocalDateTime.now().plusDays(2));
        
        Post post = new Post();
        post.setId(10L);
        post.setTitle(req.getTitle());
        post.setContent(req.getContent());
        post.setScheduledAt(req.getScheduledAt());
        post.setStatus(PostStatus.SCHEDULED);

        PostResponse res = PostResponse.from(post);
        when(postService.create(any(PostRequest.class))).thenReturn(res);

        mockMvc.perform(post("/api/posts")
                .header("X-Correlation-Id", "test-corr-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.title").value("New Release Announcement"))
                .andExpect(header().string("X-Correlation-Id", "test-corr-123"));
    }

    @Test
    @DisplayName("POST /api/posts - Validation failure returns 400 BAD_REQUEST with field error map")
    void testCreatePost_ValidationFailure() throws Exception {
        PostRequest invalidReq = new PostRequest("", "", LocalDateTime.now().minusDays(1)); // past date, blank title/content

        mockMvc.perform(post("/api/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.data.title").exists())
                .andExpect(jsonPath("$.data.content").exists())
                .andExpect(jsonPath("$.data.scheduledAt").exists());
    }

    @Test
    @DisplayName("GET /api/posts/{id} - Non-existent ID returns 404 NOT_FOUND")
    void testGetOne_NotFound() throws Exception {
        when(postService.getById(999L)).thenThrow(new PostNotFoundException(999L));

        mockMvc.perform(get("/api/posts/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("post 999 was not found"));
    }
}
