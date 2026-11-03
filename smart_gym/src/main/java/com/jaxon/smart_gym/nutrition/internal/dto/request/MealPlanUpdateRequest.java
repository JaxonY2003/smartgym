package com.jaxon.smart_gym.nutrition.internal.dto.request;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

public record MealPlanUpdateRequest(
        @Size(max = 128, message = "计划名称不能超过 128 个字符")
        String name,
        LocalDate date,
        @Size(max = 32, message = "饮食目标不能超过 32 个字符")
        String goal,
        @Size(min = 1, message = "饮食计划至少需要一个餐次")
        @Valid
        List<MealPlanCreateRequest.MealRequest> meals,
        List<@Size(max = 1000, message = "假设说明不能超过 1000 个字符") String> assumptions
) {
}
