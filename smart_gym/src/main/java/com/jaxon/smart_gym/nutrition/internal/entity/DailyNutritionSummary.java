package com.jaxon.smart_gym.nutrition.internal.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jaxon.smart_gym.common.entity.BaseEntity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("daily_nutrition_summary")
public class DailyNutritionSummary extends BaseEntity {

    private Long userId;
    private LocalDate summaryDate;
    private BigDecimal targetCalories;
    private BigDecimal targetProteinG;
    private BigDecimal targetCarbohydrateG;
    private BigDecimal targetFatG;
    private BigDecimal targetFiberG;
    private BigDecimal consumedCalories;
    private BigDecimal consumedProteinG;
    private BigDecimal consumedCarbohydrateG;
    private BigDecimal consumedFatG;
    private BigDecimal consumedFiberG;
    private Integer mealCount;
    private BigDecimal recordCompleteness;
    private LocalDateTime calculatedAt;
}
