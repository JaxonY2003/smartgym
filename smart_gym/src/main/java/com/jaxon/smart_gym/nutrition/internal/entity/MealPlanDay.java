package com.jaxon.smart_gym.nutrition.internal.entity;

import java.time.LocalDate;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jaxon.smart_gym.common.entity.BaseEntity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("meal_plan_day")
public class MealPlanDay extends BaseEntity {

    private Long mealPlanId;
    private LocalDate planDate;
    private Integer dayIndex;
}
