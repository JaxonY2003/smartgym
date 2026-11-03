package com.jaxon.smart_gym.training.internal.dto.response;

public record MuscleGroupResponse(
        Long id,
        String code,
        String name,
        Long parentId,
        Integer sortOrder,
        String status
) {
}
