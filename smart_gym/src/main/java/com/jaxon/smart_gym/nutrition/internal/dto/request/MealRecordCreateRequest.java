package com.jaxon.smart_gym.nutrition.internal.dto.request;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MealRecordCreateRequest(
        @NotNull(message = "进食时间不能为空")
        OffsetDateTime eatenAt,
        @NotNull(message = "餐次不能为空")
        @Size(max = 32, message = "餐次不能超过 32 个字符")
        String mealType,
        @NotEmpty(message = "饮食记录至少需要一项食物")
        List<@Valid Item> items,
        @Size(max = 32, message = "输入来源不能超过 32 个字符")
        String inputSource,
        @Size(max = 2000, message = "备注不能超过 2000 个字符")
        String note
) {

    public record Item(
            @NotNull(message = "食物 ID 不能为空")
            @Min(value = 1, message = "食物 ID 必须大于 0")
            Long foodId,
            @NotNull(message = "食物数量不能为空")
            @DecimalMin(value = "0.001", message = "食物数量必须大于 0")
            BigDecimal amount,
            @NotNull(message = "食物单位不能为空")
            @Size(max = 16, message = "食物单位不能超过 16 个字符")
            String unit,
            Boolean confirmedByUser
    ) {
    }
}
