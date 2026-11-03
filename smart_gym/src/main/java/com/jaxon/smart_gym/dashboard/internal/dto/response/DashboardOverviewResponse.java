package com.jaxon.smart_gym.dashboard.internal.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DashboardOverviewResponse(
        LocalDate date,
        TodayWorkout todayWorkout,
        Nutrition nutrition,
        BigDecimal weeklyWorkoutCompletionRate,
        BigDecimal latestWeightKg,
        BigDecimal weightChangeKg7d,
        List<String> quickQuestions
) {

    public record TodayWorkout(
            Long planId,
            String title,
            Integer estimatedMinutes,
            String status
    ) {
    }

    public record Nutrition(
            BigDecimal caloriesConsumed,
            BigDecimal caloriesTarget,
            BigDecimal proteinConsumedG,
            BigDecimal proteinTargetG
    ) {
    }
}
