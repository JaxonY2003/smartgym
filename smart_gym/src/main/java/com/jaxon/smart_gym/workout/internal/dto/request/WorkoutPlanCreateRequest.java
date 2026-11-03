package com.jaxon.smart_gym.workout.internal.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record WorkoutPlanCreateRequest(
        @NotBlank(message = "计划名称不能为空")
        @Size(max = 128, message = "计划名称不能超过 128 个字符")
        String name,
        @Size(max = 32, message = "训练目标不能超过 32 个字符")
        String goal,
        LocalDate startsOn,
        LocalDate endsOn,
        @Min(value = 1, message = "计划周数必须大于 0")
        Integer durationWeeks,
        @Min(value = 1, message = "每周训练天数必须大于 0")
        Integer trainingDaysPerWeek,
        @NotEmpty(message = "训练计划至少需要一个训练日")
        List<@Valid WorkoutPlanDayRequest> days
) {

    public record WorkoutPlanDayRequest(
            @Min(value = 1, message = "星期几必须大于 0")
            Integer dayOfWeek,
            @NotBlank(message = "训练日标题不能为空")
            @Size(max = 128, message = "训练日标题不能超过 128 个字符")
            String title,
            @Min(value = 1, message = "预计训练时长必须大于 0")
            Integer estimatedMinutes,
            @Size(max = 1000, message = "训练日备注不能超过 1000 个字符")
            String notes,
            @NotEmpty(message = "训练日至少需要一个动作")
            List<@Valid WorkoutPlanExerciseRequest> exercises
    ) {
    }

    public record WorkoutPlanExerciseRequest(
            @NotNull(message = "动作 ID 不能为空")
            @Min(value = 1, message = "动作 ID 必须大于 0")
            Long exerciseId,
            @Min(value = 1, message = "组数必须大于 0")
            Integer sets,
            @Min(value = 1, message = "目标次数必须大于 0")
            Integer targetRepsMin,
            @Min(value = 1, message = "目标次数必须大于 0")
            Integer targetRepsMax,
            @Min(value = 0, message = "目标重量不能小于 0")
            BigDecimal targetWeightKg,
            @Min(value = 0, message = "休息时间不能小于 0")
            Integer restSeconds,
            @Size(max = 32, message = "节奏不能超过 32 个字符")
            String tempo,
            @Size(max = 1000, message = "动作备注不能超过 1000 个字符")
            String notes
    ) {
    }
}
