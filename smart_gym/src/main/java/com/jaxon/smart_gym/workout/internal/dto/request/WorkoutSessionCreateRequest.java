package com.jaxon.smart_gym.workout.internal.dto.request;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record WorkoutSessionCreateRequest(
        @Min(value = 1, message = "计划 ID 必须大于 0")
        Long planId,
        @Min(value = 1, message = "训练日 ID 必须大于 0")
        Long planDayId,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt,
        @Size(max = 32, message = "训练状态不能超过 32 个字符")
        String status,
        @Min(value = 0, message = "疲劳程度不能小于 0")
        @Max(value = 10, message = "疲劳程度不能大于 10")
        Integer fatigueLevel,
        @Size(max = 64, message = "疼痛部位不能超过 64 个字符")
        String painBodyPart,
        @Min(value = 0, message = "疼痛程度不能小于 0")
        @Max(value = 10, message = "疼痛程度不能大于 10")
        Integer painLevel,
        @Size(max = 2000, message = "训练备注不能超过 2000 个字符")
        String note,
        @NotEmpty(message = "训练记录至少需要一组动作数据")
        List<@Valid WorkoutSetRequest> sets
) {

    public record WorkoutSetRequest(
            @NotNull(message = "动作 ID 不能为空")
            @Min(value = 1, message = "动作 ID 必须大于 0")
            Long exerciseId,
            @NotNull(message = "组序号不能为空")
            @Min(value = 1, message = "组序号必须大于 0")
            Integer setIndex,
            @Min(value = 0, message = "重量不能小于 0")
            BigDecimal weightKg,
            @Min(value = 0, message = "次数不能小于 0")
            Integer reps,
            Boolean completed,
            @Min(value = 0, message = "休息时间不能小于 0")
            Integer restSeconds,
            @Min(value = 0, message = "RPE 不能小于 0")
            @Max(value = 10, message = "RPE 不能大于 10")
            BigDecimal rpe,
            @Size(max = 500, message = "训练组备注不能超过 500 个字符")
            String note
    ) {
    }
}
