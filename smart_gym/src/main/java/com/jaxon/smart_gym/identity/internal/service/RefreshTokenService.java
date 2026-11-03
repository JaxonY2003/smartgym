package com.jaxon.smart_gym.identity.internal.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.jaxon.smart_gym.identity.internal.entity.SysRefreshToken;
import com.jaxon.smart_gym.identity.internal.mapper.SysRefreshTokenMapper;

@Service
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32;
    private static final int MAX_USER_AGENT_LENGTH = 512;
    private static final HexFormat HEX_FORMAT = HexFormat.of();

    private final SysRefreshTokenMapper refreshTokenMapper;
    private final long refreshTokenTtlSeconds;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
            SysRefreshTokenMapper refreshTokenMapper,
            @Value("${security.jwt.refresh-token-ttl-seconds}") long refreshTokenTtlSeconds,
            Clock clock) {
        this.refreshTokenMapper = refreshTokenMapper;
        this.refreshTokenTtlSeconds = refreshTokenTtlSeconds;
        this.clock = clock;
    }

    public String issue(Long userId, String userAgent, String clientIp) {
        String rawToken = generateRawToken();

        SysRefreshToken refreshToken = new SysRefreshToken();
        refreshToken.setUserId(userId);
        refreshToken.setTokenHash(hash(rawToken));
        refreshToken.setExpiresAt(LocalDateTime.now(clock).plusSeconds(refreshTokenTtlSeconds));
        refreshToken.setUserAgent(trimToLength(userAgent, MAX_USER_AGENT_LENGTH));
        refreshToken.setClientIp(trimToLength(clientIp, 64));
        refreshTokenMapper.insert(refreshToken);

        return rawToken;
    }

    public SysRefreshToken findByRawToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return null;
        }

        return refreshTokenMapper.selectOne(
                new LambdaQueryWrapper<SysRefreshToken>()
                        .eq(SysRefreshToken::getTokenHash, hash(rawToken.trim()))
        );
    }

    /**
     * 原子撤销旧令牌并创建新令牌，避免同一个 Refresh Token 被并发重复使用。
     */
    public String rotate(
            SysRefreshToken currentToken,
            String userAgent,
            String clientIp) {
        LocalDateTime now = LocalDateTime.now(clock);
        int affectedRows = refreshTokenMapper.update(
                null,
                new LambdaUpdateWrapper<SysRefreshToken>()
                        .eq(SysRefreshToken::getId, currentToken.getId())
                        .isNull(SysRefreshToken::getRevokedAt)
                        .set(SysRefreshToken::getRevokedAt, now)
        );

        if (affectedRows != 1) {
            return null;
        }

        return issue(currentToken.getUserId(), userAgent, clientIp);
    }

    public int revokeAll(Long userId) {
        return refreshTokenMapper.update(
                null,
                new LambdaUpdateWrapper<SysRefreshToken>()
                        .eq(SysRefreshToken::getUserId, userId)
                        .isNull(SysRefreshToken::getRevokedAt)
                        .set(SysRefreshToken::getRevokedAt, LocalDateTime.now(clock))
        );
    }

    public boolean isUsable(SysRefreshToken token, LocalDateTime now) {
        return token != null
                && token.getRevokedAt() == null
                && token.getExpiresAt() != null
                && token.getExpiresAt().isAfter(now);
    }

    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HEX_FORMAT.formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 算法不可用", exception);
        }
    }

    private String trimToLength(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength
                ? trimmed
                : trimmed.substring(0, maxLength);
    }
}
