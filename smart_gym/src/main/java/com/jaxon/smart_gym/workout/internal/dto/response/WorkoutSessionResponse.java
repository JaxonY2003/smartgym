package com.jaxon.smart_gym.workout.internal.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record WorkoutSessionResponse(
        Long id,
        Long planId,
        Long planDayId,
        String status,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt,
        Long durationMinutes,
        BigDecimal totalVolumeKg,
        Integer fatigueLevel,
        String painBodyPart,
        Integer painLevel,
        String note,
        List<WorkoutSetResponse> sets,
        List<WorkoutFeedbackResponse> feedback,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public record WorkoutSetResponse(
            Long id,
            Long exerciseId,
            String exerciseName,
            Integer setIndex,
            BigDecimal weightKg,
            Integer reps,
            Boolean completed,
            Integer restSeconds,
            BigDecimal rpe,
            String note
    ) {
    }

    public record WorkoutFeedbackResponse(
            Long id,
            Integer fatigueLevel,
            String painBodyPart,
            Integer painLevel,
            String feeling,
            Boolean shouldAdjustNextPlan,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
    }
}
