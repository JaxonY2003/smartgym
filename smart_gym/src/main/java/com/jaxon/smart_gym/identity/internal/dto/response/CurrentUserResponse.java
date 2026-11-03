package com.jaxon.smart_gym.identity.internal.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

public record CurrentUserResponse(
        Long id,
        String account,
        String displayName,
        String avatarUrl,
        List<String> roles,
        OffsetDateTime createdAt
) {
}
