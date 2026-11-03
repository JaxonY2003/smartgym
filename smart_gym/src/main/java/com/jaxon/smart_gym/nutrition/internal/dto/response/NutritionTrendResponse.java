package com.jaxon.smart_gym.nutrition.internal.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record NutritionTrendResponse(
        LocalDate from,
        LocalDate to,
        String metric,
        List<Point> points
) {

    public record Point(
            LocalDate date,
            BigDecimal value,
            BigDecimal target,
            BigDecimal completionRate
    ) {
    }
}
