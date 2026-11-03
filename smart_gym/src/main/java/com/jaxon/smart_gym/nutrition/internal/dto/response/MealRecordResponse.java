package com.jaxon.smart_gym.nutrition.internal.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record MealRecordResponse(
        Long id,
        OffsetDateTime eatenAt,
        String mealType,
        List<ItemResponse> items,
        NutritionResponse totalNutrition,
        String inputSource,
        String note,
        Boolean confirmedByUser,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public record ItemResponse(
            Long foodId,
            String foodNameSnapshot,
            BigDecimal amount,
            String unit,
            NutritionResponse estimatedNutrition,
            BigDecimal confidence,
            Boolean confirmedByUser
    ) {
    }

    public record NutritionResponse(
            BigDecimal calories,
            BigDecimal proteinG,
            BigDecimal carbohydrateG,
            BigDecimal fatG,
            BigDecimal fiberG
    ) {
    }
}
