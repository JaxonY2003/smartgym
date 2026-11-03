package com.jaxon.smart_gym.nutrition.internal.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record FoodResponse(
        Long id,
        String name,
        String category,
        BigDecimal defaultServingAmount,
        String defaultServingUnit,
        FoodNutritionResponse nutritionPer100g,
        List<String> allergens,
        String source,
        String sourceVersion
) {
}
