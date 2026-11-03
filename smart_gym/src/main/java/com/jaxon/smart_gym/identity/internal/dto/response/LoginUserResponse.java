package com.jaxon.smart_gym.identity.internal.dto.response;

import java.util.List;

public record LoginUserResponse(
        Long id,
        String account,
        String displayName,
        List<String> roles
) {
}
