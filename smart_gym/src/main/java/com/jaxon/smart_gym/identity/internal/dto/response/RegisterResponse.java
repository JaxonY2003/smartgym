package com.jaxon.smart_gym.identity.internal.dto.response;

public record RegisterResponse(
        Long userId,
        String account,
        String displayName
) {
}
