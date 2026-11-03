package com.jaxon.smart_gym.identity.internal.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank(message = "刷新令牌不能为空")
        String refreshToken
) {
    public RefreshTokenRequest {
        refreshToken = refreshToken == null ? null : refreshToken.trim();
    }
}
