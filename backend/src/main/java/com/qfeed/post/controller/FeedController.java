package com.qfeed.post.controller;

import com.qfeed.post.dto.FeedResponse;
import com.qfeed.post.service.PostService;
import com.qfeed.post.mapper.PostMapper;
import com.qfeed.post.mapper.PostLikeMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/feed")
public class FeedController {

    private final PostService postService;
    private final PostMapper postMapper;
    private final PostLikeMapper postLikeMapper;

    public FeedController(PostService postService, PostMapper postMapper, PostLikeMapper postLikeMapper) {
        this.postService = postService;
        this.postMapper = postMapper;
        this.postLikeMapper = postLikeMapper;
    }

    @GetMapping
    public ResponseEntity<FeedResponse> getFeed(
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "limit", required = false, defaultValue = "20") int limit,
            HttpServletRequest request
    ) {
        Object userIdAttr = request.getAttribute("userId");
        if (userIdAttr == null) {
            throw new IllegalArgumentException("unauthorized");
        }

        Long userId = (Long) userIdAttr;

        FeedResponse response = postService.getFeed(userId, cursor, limit);
        return ResponseEntity.ok(response);
    }
}