package com.jaxon.smart_gym.workout.internal.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record WorkoutFeedbackRequest(
        @Min(value = 0, message = "疲劳程度不能小于 0")
        @Max(value = 10, message = "疲劳程度不能大于 10")
        Integer fatigueLevel,
        @Size(max = 64, message = "疼痛部位不能超过 64 个字符")
        String painBodyPart,
        @Min(value = 0, message = "疼痛程度不能小于 0")
        @Max(value = 10, message = "疼痛程度不能大于 10")
        Integer painLevel,
        @Size(max = 2000, message = "主观感受不能超过 2000 个字符")
        String feeling,
        Boolean shouldAdjustNextPlan
) {
}
