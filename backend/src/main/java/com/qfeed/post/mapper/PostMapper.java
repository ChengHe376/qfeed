package com.qfeed.post.mapper;

import com.qfeed.post.domain.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PostMapper {

    int insert(Post post);

    Post findById(@Param("id") Long id);

    List<Post> listFeed(
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            @Param("limit") int limit
    );

    int softDeleteByIdAndUserId(@Param("id") Long id,
                                @Param("userId") Long userId,
                                @Param("deletedAt") LocalDateTime deletedAt);

    int increaseLikeCount(@Param("id") Long id);

    int decreaseLikeCount(@Param("id") Long id);
}