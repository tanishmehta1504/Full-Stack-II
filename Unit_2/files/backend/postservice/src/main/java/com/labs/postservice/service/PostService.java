package com.labs.postservice.service;

import com.labs.postservice.dto.PostRequest;
import com.labs.postservice.dto.PostResponse;
import com.labs.postservice.entity.Post;
import com.labs.postservice.entity.PostStatus;
import com.labs.postservice.exception.InvalidPostStateException;
import com.labs.postservice.exception.PostNotFoundException;
import com.labs.postservice.repository.PostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PostService {

    private static final Logger log = LoggerFactory.getLogger(PostService.class);

    private final PostRepository postRepository;

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Transactional
    public PostResponse create(PostRequest request) {
        Post post = new Post();
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setScheduledAt(request.getScheduledAt());
        post.setStatus(PostStatus.SCHEDULED);
        Post saved = postRepository.save(post);
        log.info("created post id={}", saved.getId());
        return PostResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public PostResponse getById(Long id) {
        return PostResponse.from(find(id));
    }

    @Transactional(readOnly = true)
    public List<PostResponse> getAll() {
        return postRepository.findAll().stream().map(PostResponse::from).toList();
    }

    @Transactional
    public PostResponse update(Long id, PostRequest request) {
        Post post = find(id);
        if (post.getStatus() == PostStatus.PUBLISHED) {
            throw new InvalidPostStateException("post " + id + " is already published and can't be edited");
        }
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setScheduledAt(request.getScheduledAt());
        Post saved = postRepository.save(post);
        log.info("updated post id={}", id);
        return PostResponse.from(saved);
    }

    @Transactional
    public void delete(Long id) {
        Post post = find(id);
        postRepository.delete(post);
        log.info("deleted post id={}", id);
    }

    @Transactional
    public PostResponse publishNow(Long id) {
        Post post = find(id);
        if (post.getStatus() == PostStatus.PUBLISHED) {
            throw new InvalidPostStateException("post " + id + " is already published");
        }
        post.setStatus(PostStatus.PUBLISHED);
        post.setPublishedAt(LocalDateTime.now());
        Post saved = postRepository.save(post);
        log.info("published post id={}", id);
        return PostResponse.from(saved);
    }

    private Post find(Long id) {
        return postRepository.findById(id).orElseThrow(() -> new PostNotFoundException(id));
    }
}
