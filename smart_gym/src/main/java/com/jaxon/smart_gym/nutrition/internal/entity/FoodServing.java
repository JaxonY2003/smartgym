package com.jaxon.smart_gym.nutrition.internal.entity;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jaxon.smart_gym.common.entity.BaseEntity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("food_serving")
public class FoodServing extends BaseEntity {

    private Long foodId;
    private String name;
    private BigDecimal amount;
    private String unit;
    private BigDecimal gramWeight;
    private Boolean isDefault;
}
