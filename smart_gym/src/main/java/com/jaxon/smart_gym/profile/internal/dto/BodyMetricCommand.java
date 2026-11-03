package com.jaxon.smart_gym.profile.internal.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record BodyMetricCommand(
        OffsetDateTime measuredAt,
        BigDecimal weightKg,
        BigDecimal bodyFatPercent,
        BigDecimal waistCm,
        String note
) {
}
