package com.jaxon.smart_gym.training.internal.dto.response;

public record EquipmentResponse(
        Long id,
        String name,
        String category,
        String status,
        String brand,
        String description
) {
}
