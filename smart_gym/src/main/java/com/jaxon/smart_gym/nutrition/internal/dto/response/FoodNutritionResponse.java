package com.jaxon.smart_gym.nutrition.internal.dto.response;

import java.math.BigDecimal;

public record FoodNutritionResponse(
        BigDecimal calories,
        BigDecimal protein,
        BigDecimal carbohydrate,
        BigDecimal fat,
        BigDecimal fiber,
        BigDecimal sugar,
        BigDecimal sodium
) {
}
