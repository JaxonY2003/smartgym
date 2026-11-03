package com.jaxon.smart_gym.nutrition.internal.dto.response;

import java.time.OffsetDateTime;

public record WaterRecordResponse(
        Long id,
        OffsetDateTime recordedAt,
        Integer amountMl,
        String source,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
