package com.qfeed.post.service;

import com.qfeed.post.domain.Post;
import com.qfeed.post.dto.CreatePostRequest;
import com.qfeed.post.dto.FeedResponse;
import com.qfeed.post.dto.PostItem;
import com.qfeed.post.mapper.PostMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class PostService {

    private final PostMapper postMapper;

    public PostService(PostMapper postMapper) {
        this.postMapper = postMapper;
    }

    @Transactional
    public PostItem createPost(Long userId, String username, CreatePostRequest req) {
        String content = req.content == null ? "" : req.content.trim();
        if (content.isBlank()) {
            throw new IllegalArgumentException("content required");
        }

        Post post = new Post();
        post.userId = userId;
        post.content = content;

        int rows = postMapper.insert(post);
        if (rows != 1 || post.id == null) {
            throw new IllegalStateException("failed to create post");
        }

        Post saved = postMapper.findById(post.id);
        if (saved == null) {
            throw new IllegalStateException("post not found after insert");
        }

        return toPostItem(saved, username);
    }

    public FeedResponse getFeed(String cursor, int limit) {
        int safeLimit = normalizeLimit(limit);

        CursorValue cursorValue = decodeCursor(cursor);

        List<Post> posts = postMapper.listFeed(
                cursorValue == null ? null : cursorValue.createdAt,
                cursorValue == null ? null : cursorValue.id,
                safeLimit
        );

        List<PostItem> items = new ArrayList<>();
        for (Post post : posts) {
            items.add(toPostItem(post, null));
        }

        String nextCursor = null;
        if (!posts.isEmpty()) {
            Post last = posts.get(posts.size() - 1);
            nextCursor = encodeCursor(last.createdAt, last.id);
        }

        return new FeedResponse(items, nextCursor);
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return 20;
        }
        return Math.min(limit, 50);
    }

    private PostItem toPostItem(Post post, String username) {
        PostItem item = new PostItem();
        item.id = post.id;
        item.userId = post.userId;
        item.username = username;
        item.content = post.content;
        item.createdAt = post.createdAt;
        return item;
    }

    private String encodeCursor(LocalDateTime createdAt, Long id) {
        long epochMilli = createdAt.toInstant(ZoneOffset.UTC).toEpochMilli();
        String raw = epochMilli + "|" + id;
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private CursorValue decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }

        try {
            byte[] decoded = Base64.getUrlDecoder().decode(cursor);
            String raw = new String(decoded, StandardCharsets.UTF_8);

            String[] parts = raw.split("\\|");
            if (parts.length != 2) {
                throw new IllegalArgumentException("invalid cursor");
            }

            long epochMilli = Long.parseLong(parts[0]);
            long id = Long.parseLong(parts[1]);

            LocalDateTime createdAt = LocalDateTime.ofEpochSecond(
                    epochMilli / 1000,
                    (int) ((epochMilli % 1000) * 1_000_000),
                    ZoneOffset.UTC
            );

            return new CursorValue(createdAt, id);
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid cursor");
        }
    }

    private static class CursorValue {
        private final LocalDateTime createdAt;
        private final Long id;

        private CursorValue(LocalDateTime createdAt, Long id) {
            this.createdAt = createdAt;
            this.id = id;
        }
    }
}