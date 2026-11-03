package com.jaxon.smart_gym.profile.internal.dto;

import java.util.List;

public record NutritionProfileCommand(
        String dietType,
        List<String> allergies,
        List<String> restrictions,
        List<String> dislikedFoods,
        List<String> preferredCuisines,
        Integer dailyWaterMlTarget,
        String note
) {
}
