package com.jaxon.smart_gym.identity.internal.dto.response;

public record RefreshTokenResponse(
        String accessToken,
        String refreshToken,
        long expiresIn
) {
}
