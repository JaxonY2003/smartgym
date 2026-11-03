package com.jaxon.smart_gym.profile.internal.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record BodyMetricResponse(
        Long id,
        OffsetDateTime measuredAt,
        BigDecimal weightKg,
        BigDecimal bodyFatPercent,
        BigDecimal waistCm,
        String note
) {
}
