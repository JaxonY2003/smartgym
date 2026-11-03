package com.jaxon.smart_gym.nutrition.internal.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MealPlanReplaceItemRequest(
        @NotNull(message = "原食物项 ID 不能为空")
        @Min(value = 1, message = "原食物项 ID 必须大于 0")
        Long itemId,
        @NotNull(message = "替换食物 ID 不能为空")
        @Min(value = 1, message = "替换食物 ID 必须大于 0")
        Long replacementFoodId,
        @Size(max = 500, message = "替换原因不能超过 500 个字符")
        String reason
) {
}
