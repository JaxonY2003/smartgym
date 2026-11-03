package com.jaxon.smart_gym.nutrition.internal.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

public record MealPlanResponse(
        Long id,
        String name,
        LocalDate date,
        String source,
        String goal,
        String status,
        EstimatedNutrition estimatedNutrition,
        List<String> warnings,
        List<String> assumptions,
        Integer version,
        List<DayResponse> days,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public record DayResponse(
            Long id,
            LocalDate date,
            Integer dayIndex,
            List<MealResponse> meals
    ) {
    }

    public record MealResponse(
            Long id,
            String mealType,
            LocalTime time,
            String notes,
            List<ItemResponse> items
    ) {
    }

    public record ItemResponse(
            Long id,
            Long foodId,
            String foodNameSnapshot,
            BigDecimal amount,
            String unit,
            EstimatedNutrition estimatedNutrition,
            String note,
            Integer sortOrder
    ) {
    }

    public record EstimatedNutrition(
            BigDecimal calories,
            BigDecimal proteinG,
            BigDecimal carbohydrateG,
            BigDecimal fatG,
            BigDecimal fiberG
    ) {
    }
}
