package com.jaxon.smart_gym.profile.internal.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record MemberProfileView(
        Long userId,
        String gender,
        LocalDate birthDate,
        BigDecimal heightCm,
        BigDecimal weightKg,
        String trainingExperience,
        Integer weeklyTrainingDays,
        Integer maxSessionMinutes,
        List<String> goals,
        String medicalNotice,
        String note,
        OffsetDateTime updatedAt
) {
}
