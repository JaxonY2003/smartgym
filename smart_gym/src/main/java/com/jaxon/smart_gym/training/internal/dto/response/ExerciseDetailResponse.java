package com.jaxon.smart_gym.training.internal.dto.response;

import java.util.List;

public record ExerciseDetailResponse(
        Long id,
        String name,
        String englishName,
        String description,
        String instructions,
        List<String> primaryMuscles,
        List<EquipmentSummaryResponse> equipment,
        String difficulty,
        Integer defaultRestSeconds,
        List<String> contraindications,
        String thumbnailUrl
) {
}
