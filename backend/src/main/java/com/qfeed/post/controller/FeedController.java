package com.qfeed.post.controller;

import com.qfeed.post.dto.FeedResponse;
import com.qfeed.post.service.PostService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feed")
public class FeedController {

    private final PostService postService;

    public FeedController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    public ResponseEntity<FeedResponse> getFeed(
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "limit", required = false, defaultValue = "20") int limit
    ) {
        FeedResponse response = postService.getFeed(cursor, limit);
        return ResponseEntity.ok(response);
    }
}