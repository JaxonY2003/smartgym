package com.jaxon.smart_gym.profile.internal.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

public record NutritionProfileResponse(
        Long userId,
        String dietType,
        List<String> allergies,
        List<String> restrictions,
        List<String> dislikedFoods,
        List<String> preferredCuisines,
        Integer dailyWaterMlTarget,
        String note,
        OffsetDateTime updatedAt
) {
}
