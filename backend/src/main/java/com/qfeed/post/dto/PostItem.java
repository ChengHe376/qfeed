package com.qfeed.post.dto;

import java.time.LocalDateTime;

public class PostItem {
    public Long id;
    public Long userId;
    public String username;
    public String content;
    public LocalDateTime createdAt;
    public Integer likeCount;
    public Boolean likedByMe;

    public PostItem() {
    }

    public PostItem(Long id, Long userId, String username, String content, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.content = content;
        this.createdAt = createdAt;
        this.likeCount = likeCount;
        this.likedByMe = likedByMe;
    }
}