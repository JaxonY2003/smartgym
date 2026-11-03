package com.jaxon.smart_gym.workout.internal.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record WorkoutPlanResponse(
        Long id,
        String name,
        String source,
        String status,
        String goal,
        LocalDate startsOn,
        LocalDate endsOn,
        Integer durationWeeks,
        Integer trainingDaysPerWeek,
        Integer version,
        List<String> warnings,
        List<String> assumptions,
        List<WorkoutPlanDayResponse> days,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public record WorkoutPlanDayResponse(
            Long id,
            Integer dayNo,
            Integer dayOfWeek,
            String title,
            Integer estimatedMinutes,
            String notes,
            List<WorkoutPlanExerciseResponse> exercises
    ) {
    }

    public record WorkoutPlanExerciseResponse(
            Long id,
            Long exerciseId,
            String exerciseName,
            Integer sortOrder,
            Integer sets,
            Integer targetRepsMin,
            Integer targetRepsMax,
            BigDecimal targetWeightKg,
            Integer restSeconds,
            String tempo,
            String notes
    ) {
    }
}
