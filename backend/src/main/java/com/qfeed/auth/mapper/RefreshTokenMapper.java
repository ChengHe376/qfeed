package com.qfeed.auth.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.qfeed.auth.security.RefreshTokenRecord;

@Mapper
public interface RefreshTokenMapper {

    int insert(
            @Param("userId") Long userId,
            @Param("jti") String jti,
            @Param("tokenHash") String tokenHash,
            @Param("expiresAt") java.time.LocalDateTime expiresAt);

    RefreshTokenRecord findByJti(@Param("jti") String jti);

    int revokeByJti(
            @Param("jti") String jti,
            @Param("revokedAt") java.time.LocalDateTime revokedAt,
            @Param("replacedByJti") String replacedByJti);

    int revokeByJtiForce(
            @Param("jti") String jti,
            @Param("revokedAt") java.time.LocalDateTime revokedAt
    );

    int revokeAllByUserId(
            @Param("userId") Long userId,
            @Param("revokedAt") java.time.LocalDateTime revokedAt);

    RefreshTokenRecord findActiveByJti(@Param("jti") String jti);
}
