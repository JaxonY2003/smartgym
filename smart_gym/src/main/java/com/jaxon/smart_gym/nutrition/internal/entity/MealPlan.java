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
@TableName("meal_plan")
public class MealPlan extends BaseEntity {

    private Long userId;
    private String name;
    private String source;
    private String goal;
    private String status;
    private LocalDate startsOn;
    private LocalDate endsOn;
    private BigDecimal estimatedCalories;
    private BigDecimal estimatedProteinG;
    private BigDecimal estimatedCarbohydrateG;
    private BigDecimal estimatedFatG;
    private BigDecimal estimatedFiberG;
    private String warningsJson;
    private String assumptionsJson;
    private String confirmedRunId;
    private Integer version;
    private LocalDateTime deletedAt;
}
