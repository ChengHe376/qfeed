package com.qfeed.post.domain;

import java.time.LocalDateTime;

public class Post {
    public Long id;
    public Long userId;
    public String content;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}