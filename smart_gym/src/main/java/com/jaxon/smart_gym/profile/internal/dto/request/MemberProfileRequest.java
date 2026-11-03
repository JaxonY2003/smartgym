package com.jaxon.smart_gym.profile.internal.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MemberProfileRequest(
        @NotNull(message = "性别不能为空")
        @Pattern(regexp = "MALE|FEMALE|OTHER|UNKNOWN", message = "性别枚举值不正确")
        String gender,

        @NotNull(message = "出生日期不能为空")
        @PastOrPresent(message = "出生日期不能晚于今天")
        LocalDate birthDate,

        @NotNull(message = "身高不能为空")
        @DecimalMin(value = "50", message = "身高不能小于 50cm")
        @DecimalMax(value = "250", message = "身高不能大于 250cm")
        BigDecimal heightCm,

        @NotNull(message = "体重不能为空")
        @DecimalMin(value = "20", message = "体重不能小于 20kg")
        @DecimalMax(value = "500", message = "体重不能大于 500kg")
        BigDecimal weightKg,

        @NotNull(message = "训练经验不能为空")
        @Pattern(regexp = "BEGINNER|INTERMEDIATE|ADVANCED", message = "训练经验枚举值不正确")
        String trainingExperience,

        @NotNull(message = "每周训练天数不能为空")
        @Min(value = 0, message = "每周训练天数不能小于 0")
        @Max(value = 7, message = "每周训练天数不能大于 7")
        Integer weeklyTrainingDays,

        @NotNull(message = "单次训练时间不能为空")
        @Min(value = 1, message = "单次训练时间必须大于 0")
        @Max(value = 1440, message = "单次训练时间不能超过 1440 分钟")
        Integer maxSessionMinutes,

        @NotNull(message = "训练目标列表不能为空")
        List<@Pattern(regexp = "MUSCLE_GAIN|FAT_LOSS|BODY_SHAPING|MAINTENANCE", message = "训练目标枚举值不正确") String> goals,

        @Size(max = 1000, message = "医疗提示不能超过 1000 个字符")
        String medicalNotice,

        @Size(max = 1000, message = "备注不能超过 1000 个字符")
        String note
) {
}
