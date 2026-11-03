package com.jaxon.smart_gym.profile.internal.dto.response;

import java.time.LocalDate;

public record HealthConstraintResponse(
        Long id,
        String type,
        String bodyPart,
        String severity,
        String description,
        Boolean active,
        LocalDate startedOn,
        LocalDate endedOn
) {
}
