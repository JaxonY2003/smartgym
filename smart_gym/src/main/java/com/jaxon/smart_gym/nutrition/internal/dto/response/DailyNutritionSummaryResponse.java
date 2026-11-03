package com.jaxon.smart_gym.nutrition.internal.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyNutritionSummaryResponse(
        LocalDate date,
        Target target,
        Consumed consumed,
        CompletionRate completionRate,
        Integer mealCount,
        BigDecimal recordCompleteness
) {

    public record Target(
            BigDecimal calories,
            BigDecimal proteinG,
            BigDecimal carbohydrateG,
            BigDecimal fatG,
            BigDecimal fiberG
    ) {
    }

    public record Consumed(
            BigDecimal calories,
            BigDecimal proteinG,
            BigDecimal carbohydrateG,
            BigDecimal fatG,
            BigDecimal fiberG
    ) {
    }

    public record CompletionRate(
            BigDecimal calories,
            BigDecimal protein,
            BigDecimal carbohydrate,
            BigDecimal fat
    ) {
    }
}
