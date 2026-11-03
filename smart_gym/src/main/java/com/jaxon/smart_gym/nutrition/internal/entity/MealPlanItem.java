package com.jaxon.smart_gym.nutrition.internal.entity;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jaxon.smart_gym.common.entity.BaseEntity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("meal_plan_item")
public class MealPlanItem extends BaseEntity {

    private Long mealPlanMealId;
    private Long foodId;
    private Long recipeId;
    private String foodNameSnapshot;
    private BigDecimal amount;
    private String unit;
    private BigDecimal estimatedCalories;
    private BigDecimal estimatedProteinG;
    private BigDecimal estimatedCarbohydrateG;
    private BigDecimal estimatedFatG;
    private BigDecimal estimatedFiberG;
    private String note;
    private Integer sortOrder;
}
