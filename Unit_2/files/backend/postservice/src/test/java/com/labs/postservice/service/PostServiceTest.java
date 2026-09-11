package com.labs.postservice.service;

import com.labs.postservice.dto.PostRequest;
import com.labs.postservice.dto.PostResponse;
import com.labs.postservice.entity.Post;
import com.labs.postservice.entity.PostStatus;
import com.labs.postservice.exception.InvalidPostStateException;
import com.labs.postservice.exception.PostNotFoundException;
import com.labs.postservice.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private PostService postService;

    private Post samplePost;
    private PostRequest sampleRequest;

    @BeforeEach
    void setUp() {
        samplePost = new Post();
        samplePost.setId(1L);
        samplePost.setTitle("Test Title");
        samplePost.setContent("Test Content");
        samplePost.setScheduledAt(LocalDateTime.now().plusDays(1));
        samplePost.setStatus(PostStatus.SCHEDULED);

        sampleRequest = new PostRequest(
                "Test Title",
                "Test Content",
                LocalDateTime.now().plusDays(1)
        );
    }

    @Test
    @DisplayName("Create post successfully with SCHEDULED status")
    void testCreatePost_Success() {
        when(postRepository.save(any(Post.class))).thenReturn(samplePost);

        PostResponse response = postService.create(sampleRequest);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Test Title", response.getTitle());
        assertEquals(PostStatus.SCHEDULED, response.getStatus());
        verify(postRepository, times(1)).save(any(Post.class));
    }

    @Test
    @DisplayName("Get post by ID successfully")
    void testGetById_Success() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(samplePost));

        PostResponse response = postService.getById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        verify(postRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Get post by ID throws PostNotFoundException when not found")
    void testGetById_NotFound() {
        when(postRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(PostNotFoundException.class, () -> postService.getById(99L));
        verify(postRepository, times(1)).findById(99L);
    }

    @Test
    @DisplayName("Publish post successfully when status is SCHEDULED")
    void testPublishNow_Success() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(samplePost));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post p = invocation.getArgument(0);
            return p;
        });

        PostResponse response = postService.publishNow(1L);

        assertEquals(PostStatus.PUBLISHED, response.getStatus());
        assertNotNull(response.getPublishedAt());
    }

    @Test
    @DisplayName("Publish post throws InvalidPostStateException if already PUBLISHED")
    void testPublishNow_AlreadyPublished() {
        samplePost.setStatus(PostStatus.PUBLISHED);
        when(postRepository.findById(1L)).thenReturn(Optional.of(samplePost));

        assertThrows(InvalidPostStateException.class, () -> postService.publishNow(1L));
    }

    @Test
    @DisplayName("Delete post successfully")
    void testDeletePost_Success() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(samplePost));
        doNothing().when(postRepository).delete(samplePost);

        assertDoesNotThrow(() -> postService.delete(1L));
        verify(postRepository, times(1)).delete(samplePost);
    }
}
