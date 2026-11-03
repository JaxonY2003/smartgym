package com.jaxon.smart_gym.nutrition.internal.dto.request;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record WaterRecordCreateRequest(
        @NotNull(message = "饮水时间不能为空")
        OffsetDateTime recordedAt,
        @NotNull(message = "饮水量不能为空")
        @Min(value = 1, message = "饮水量必须大于 0")
        Integer amountMl
) {
}
