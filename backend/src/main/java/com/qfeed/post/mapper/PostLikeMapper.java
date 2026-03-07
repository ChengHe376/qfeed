package com.qfeed.post.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface PostLikeMapper {

    int insertLike(@Param("postId") Long postId, @Param("userId") Long userId);

    int deleteLike(@Param("postId") Long postId, @Param("userId") Long userId);

    Long existsLike(@Param("postId") Long postId, @Param("userId") Long userId);

    List<Long> findLikedPostIds(@Param("userId") Long userId,
                                @Param("postIds") List<Long> postIds);
}