package com.jaxon.smart_gym.workout.internal.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record WorkoutPlanUpdateRequest(
        @Size(max = 128, message = "计划名称不能超过 128 个字符")
        String name,
        @Size(max = 32, message = "训练目标不能超过 32 个字符")
        String goal,
        @Size(max = 32, message = "计划状态不能超过 32 个字符")
        String status,
        LocalDate startsOn,
        LocalDate endsOn,
        @Min(value = 1, message = "计划周数必须大于 0")
        Integer durationWeeks,
        @Min(value = 1, message = "每周训练天数必须大于 0")
        Integer trainingDaysPerWeek
) {
}
