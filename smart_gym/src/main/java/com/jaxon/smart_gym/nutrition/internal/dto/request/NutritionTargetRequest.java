package com.jaxon.smart_gym.nutrition.internal.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

public record NutritionTargetRequest(
        @DecimalMin(value = "0", message = "热量目标不能小于 0")
        BigDecimal calories,
        @DecimalMin(value = "0", message = "蛋白质目标不能小于 0")
        BigDecimal proteinG,
        @DecimalMin(value = "0", message = "碳水目标不能小于 0")
        BigDecimal carbohydrateG,
        @DecimalMin(value = "0", message = "脂肪目标不能小于 0")
        BigDecimal fatG,
        @DecimalMin(value = "0", message = "膳食纤维目标不能小于 0")
        BigDecimal fiberG,
        @Size(max = 32, message = "目标来源不能超过 32 个字符")
        String source
) {
}
