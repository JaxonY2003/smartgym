package com.jaxon.smart_gym.nutrition.internal.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record NutritionTargetResponse(
        Long id,
        LocalDate effectiveDate,
        String goal,
        BigDecimal calories,
        BigDecimal proteinG,
        BigDecimal carbohydrateG,
        BigDecimal fatG,
        BigDecimal fiberG,
        String source,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
