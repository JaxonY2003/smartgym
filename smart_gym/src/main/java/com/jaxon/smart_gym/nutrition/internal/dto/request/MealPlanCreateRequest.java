package com.jaxon.smart_gym.nutrition.internal.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MealPlanCreateRequest(
        @NotBlank(message = "计划名称不能为空")
        @Size(max = 128, message = "计划名称不能超过 128 个字符")
        String name,
        @NotNull(message = "计划日期不能为空")
        LocalDate date,
        @Size(max = 32, message = "饮食目标不能超过 32 个字符")
        String goal,
        @NotEmpty(message = "饮食计划至少需要一个餐次")
        List<@Valid MealRequest> meals,
        List<@Size(max = 1000, message = "假设说明不能超过 1000 个字符") String> assumptions
) {

    public record MealRequest(
            @NotBlank(message = "餐次不能为空")
            @Size(max = 32, message = "餐次不能超过 32 个字符")
            String mealType,
            LocalTime time,
            @NotEmpty(message = "餐次至少需要一个食物")
            List<@Valid ItemRequest> items,
            @Size(max = 1000, message = "餐次备注不能超过 1000 个字符")
            String notes
    ) {
    }

    public record ItemRequest(
            @NotNull(message = "食物 ID 不能为空")
            @Min(value = 1, message = "食物 ID 必须大于 0")
            Long foodId,
            @NotNull(message = "食物数量不能为空")
            @DecimalMin(value = "0.001", message = "食物数量必须大于 0")
            BigDecimal amount,
            @NotBlank(message = "食物单位不能为空")
            @Size(max = 16, message = "食物单位不能超过 16 个字符")
            String unit,
            @Size(max = 1000, message = "食物备注不能超过 1000 个字符")
            String note
    ) {
    }
}
