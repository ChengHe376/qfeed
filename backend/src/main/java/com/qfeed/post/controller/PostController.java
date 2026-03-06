package com.qfeed.post.controller;

import com.qfeed.post.dto.CreatePostRequest;
import com.qfeed.post.dto.PostItem;
import com.qfeed.post.service.PostService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping
    public ResponseEntity<PostItem> createPost(
            @Valid @RequestBody CreatePostRequest req,
            HttpServletRequest request
    ) {
        Object userIdAttr = request.getAttribute("userId");
        Object usernameAttr = request.getAttribute("username");

        if (userIdAttr == null || usernameAttr == null) {
            throw new IllegalArgumentException("unauthorized");
        }

        Long userId = (Long) userIdAttr;
        String username = String.valueOf(usernameAttr);

        PostItem item = postService.createPost(userId, username, req);
        return ResponseEntity.ok(item);
    }
}