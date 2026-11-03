package com.jaxon.smart_gym.profile.internal.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MemberProfileCommand(
        String gender,
        LocalDate birthDate,
        BigDecimal heightCm,
        BigDecimal weightKg,
        String trainingExperience,
        Integer weeklyTrainingDays,
        Integer maxSessionMinutes,
        List<String> goals,
        String medicalNotice,
        String note
) {
}
