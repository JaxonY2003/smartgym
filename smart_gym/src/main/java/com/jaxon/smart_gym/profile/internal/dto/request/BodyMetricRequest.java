package com.jaxon.smart_gym.profile.internal.dto.request;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BodyMetricRequest(
        @NotNull(message = "测量时间不能为空")
        OffsetDateTime measuredAt,

        @DecimalMin(value = "20", message = "体重不能小于 20kg")
        @DecimalMax(value = "500", message = "体重不能大于 500kg")
        BigDecimal weightKg,

        @DecimalMin(value = "0", message = "体脂率不能小于 0")
        @DecimalMax(value = "100", message = "体脂率不能大于 100")
        BigDecimal bodyFatPercent,

        @DecimalMin(value = "20", message = "腰围不能小于 20cm")
        @DecimalMax(value = "300", message = "腰围不能大于 300cm")
        BigDecimal waistCm,

        @Size(max = 1000, message = "备注不能超过 1000 个字符")
        String note
) {

    @AssertTrue(message = "至少填写一项身体指标")
    public boolean hasAtLeastOneMetric() {
        return weightKg != null || bodyFatPercent != null || waistCm != null;
    }
}
