package com.jaxon.smart_gym.profile.internal.dto.request;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record HealthConstraintRequest(
        @NotNull(message = "健康限制列表不能为空")
        List<@Valid ConstraintItem> constraints,

        @Size(max = 1000, message = "医疗提示不能超过 1000 个字符")
        String medicalNotice
) {

    public record ConstraintItem(
            @NotBlank(message = "健康限制类型不能为空")
            @Pattern(regexp = "PAIN|INJURY|CHRONIC_DISEASE|PREGNANCY|OTHER", message = "健康限制类型不正确")
            String type,
            @Size(max = 64, message = "身体部位不能超过 64 个字符") String bodyPart,
            @Size(max = 32, message = "严重程度不能超过 32 个字符") String severity,
            @Size(max = 1000, message = "描述不能超过 1000 个字符") String description,
            Boolean active,
            LocalDate startedOn,
            LocalDate endedOn
    ) {
    }
}
