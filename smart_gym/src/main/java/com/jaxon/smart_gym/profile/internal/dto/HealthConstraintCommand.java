package com.jaxon.smart_gym.profile.internal.dto;

import java.time.LocalDate;

public record HealthConstraintCommand(
        String type,
        String bodyPart,
        String severity,
        String description,
        Boolean active,
        LocalDate startedOn,
        LocalDate endedOn
) {
}
