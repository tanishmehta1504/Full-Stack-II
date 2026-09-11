package com.labs.postservice.dto;

import com.labs.postservice.entity.Post;
import com.labs.postservice.entity.PostStatus;
import java.time.LocalDateTime;

public class PostResponse {

    private Long id;
    private String title;
    private String content;
    private LocalDateTime scheduledAt;
    private PostStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;

    public static PostResponse from(Post post) {
        PostResponse r = new PostResponse();
        r.id = post.getId();
        r.title = post.getTitle();
        r.content = post.getContent();
        r.scheduledAt = post.getScheduledAt();
        r.status = post.getStatus();
        r.createdAt = post.getCreatedAt();
        r.publishedAt = post.getPublishedAt();
        return r;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public PostStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
}
