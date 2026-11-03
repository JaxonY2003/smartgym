package com.jaxon.smart_gym.nutrition.internal.entity;

import java.time.LocalTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jaxon.smart_gym.common.entity.BaseEntity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("meal_plan_meal")
public class MealPlanMeal extends BaseEntity {

    private Long mealPlanDayId;
    private String mealType;
    private LocalTime mealTime;
    private Integer sortOrder;
    private String notes;
}
