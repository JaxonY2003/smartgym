package com.jaxon.smart_gym.profile.internal.dto.request;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record NutritionProfileRequest(
        @NotNull(message = "饮食类型不能为空")
        @Pattern(regexp = "OMNIVORE|VEGETARIAN|VEGAN|PESCATARIAN|OTHER", message = "饮食类型不正确")
        String dietType,

        @NotNull(message = "过敏原列表不能为空")
        List<@Size(max = 64, message = "过敏原编码不能超过 64 个字符") String> allergies,

        @NotNull(message = "饮食限制列表不能为空")
        List<@Size(max = 64, message = "饮食限制编码不能超过 64 个字符") String> restrictions,

        @NotNull(message = "忌口食物列表不能为空")
        List<@Size(max = 255, message = "忌口食物名称不能超过 255 个字符") String> dislikedFoods,

        @NotNull(message = "偏好菜系列表不能为空")
        List<@Size(max = 255, message = "偏好菜系不能超过 255 个字符") String> preferredCuisines,

        @Min(value = 0, message = "每日饮水目标不能小于 0")
        @Max(value = 10000, message = "每日饮水目标不能超过 10000ml")
        Integer dailyWaterMlTarget,

        @Size(max = 1000, message = "备注不能超过 1000 个字符")
        String note
) {
}
