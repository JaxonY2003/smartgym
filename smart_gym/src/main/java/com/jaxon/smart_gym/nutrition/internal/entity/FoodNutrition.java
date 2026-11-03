package com.jaxon.smart_gym.nutrition.internal.entity;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jaxon.smart_gym.common.entity.BaseEntity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("food_nutrition")
public class FoodNutrition extends BaseEntity {

    private Long foodId;
    private BigDecimal servingAmount;
    private String servingUnit;
    private BigDecimal calories;
    private BigDecimal proteinG;
    private BigDecimal carbohydrateG;
    private BigDecimal fatG;
    private BigDecimal fiberG;
    private BigDecimal sodiumMg;
    private BigDecimal sugarG;
    private String source;
    private String sourceVersion;
}
