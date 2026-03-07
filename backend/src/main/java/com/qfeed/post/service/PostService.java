package com.qfeed.post.service;

import com.qfeed.post.domain.Post;
import com.qfeed.post.dto.CreatePostRequest;
import com.qfeed.post.dto.FeedResponse;
import com.qfeed.post.dto.PostItem;
import com.qfeed.post.mapper.PostMapper;
import com.qfeed.post.security.PostGuardService;
import com.qfeed.post.security.PostIdempotencyService;
import com.qfeed.post.mapper.PostLikeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DuplicateKeyException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

@Service
public class PostService {

    private final PostMapper postMapper;
    private final PostGuardService postGuardService;
    private final PostIdempotencyService postIdempotencyService;
    private final PostLikeMapper postLikeMapper;

    public PostService(PostMapper postMapper, PostGuardService postGuardService, PostIdempotencyService postIdempotencyService, PostLikeMapper postLikeMapper) {
        this.postMapper = postMapper;
        this.postGuardService = postGuardService;
        this.postIdempotencyService = postIdempotencyService;
        this.postLikeMapper = postLikeMapper;
    }

    @Transactional
    public PostItem createPost(Long userId, String username, CreatePostRequest req) {

        postGuardService.checkCreatePost(userId);

        String content = req.content == null ? "" : req.content.trim();
        if (content.isBlank()) {
            throw new IllegalArgumentException("content required");
        }

        String clientRequestId = req.clientRequestId == null ? "" : req.clientRequestId.trim();
        if (clientRequestId.isBlank()) {
            throw new IllegalArgumentException("clientRequestId required");
        }

        Long existingPostId = postIdempotencyService.getExistingPostId(userId, clientRequestId);
        if (existingPostId != null) {
            Post existing = postMapper.findById(existingPostId);
            if (existing != null) {
                return toPostItem(existing, username);
            }
        }

        boolean locked = postIdempotencyService.tryAcquireLock(userId, clientRequestId);

        if (!locked) {
            for (int i = 0; i < 10; i++) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("request interrupted");
                }

                Long postId = postIdempotencyService.getExistingPostId(userId, clientRequestId);
                if (postId != null) {
                    Post existing = postMapper.findById(postId);
                    if (existing != null) {
                        return toPostItem(existing, username);
                    }
                }
            }

            throw new IllegalArgumentException("duplicate request is processing, please retry later");
        }

        try {
            existingPostId = postIdempotencyService.getExistingPostId(userId, clientRequestId);
            if (existingPostId != null) {
                Post existing = postMapper.findById(existingPostId);
                if (existing != null) {
                    return toPostItem(existing, username);
                }
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

            postIdempotencyService.recordSuccess(userId, clientRequestId, saved.id);

            return toPostItem(saved, username);
        } finally {
            postIdempotencyService.releaseLock(userId, clientRequestId);
        }
    }

    @Transactional
    public void deletePost(Long userId, Long postId) {
        if (postId == null) {
            throw new IllegalArgumentException("postId required");
        }

        int rows = postMapper.softDeleteByIdAndUserId(postId, userId, LocalDateTime.now());
        if (rows != 1) {
            throw new IllegalArgumentException("post not found");
        }
    }

    @Transactional
    public void likePost(Long userId, Long postId) {
        if (postId == null) {
            throw new IllegalArgumentException("postId required");
        }

        Post post = postMapper.findById(postId);
        if (post == null) {
            throw new IllegalArgumentException("post not found");
        }

        try {
            int rows = postLikeMapper.insertLike(postId, userId);
            if (rows == 1) {
                postMapper.increaseLikeCount(postId);
            }
        } catch (DuplicateKeyException e) {
            // 幂等处理：同一用户重复点赞同一帖子，直接视为成功
            return;
        }
    }

    @Transactional
    public void unlikePost(Long userId, Long postId) {
        if (postId == null) {
            throw new IllegalArgumentException("postId required");
        }

        int rows = postLikeMapper.deleteLike(postId, userId);
        if (rows == 1) {
            postMapper.decreaseLikeCount(postId);
        }
    }

    public FeedResponse getFeed(Long currentUserId, String cursor, int limit) {
        int safeLimit = normalizeLimit(limit);

        CursorValue cursorValue = decodeCursor(cursor);

        List<Post> posts = postMapper.listFeed(
                cursorValue == null ? null : cursorValue.createdAt,
                cursorValue == null ? null : cursorValue.id,
                safeLimit
        );

        Set<Long> likedPostIds = new HashSet<>();

        if (currentUserId != null && !posts.isEmpty()) {
            List<Long> postIds = new ArrayList<>();
            for (Post post : posts) {
                postIds.add(post.id);
            }

            List<Long> likedIds = postLikeMapper.findLikedPostIds(currentUserId, postIds);
            likedPostIds.addAll(likedIds);
        }

        List<PostItem> items = new ArrayList<>();
        for (Post post : posts) {
            PostItem item = toPostItem(post, null);
            item.likedByMe = likedPostIds.contains(post.id);
            items.add(item);
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
        item.likeCount = post.likeCount;
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