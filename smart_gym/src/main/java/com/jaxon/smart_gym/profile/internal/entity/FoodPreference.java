package com.jaxon.smart_gym.profile.internal.entity;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jaxon.smart_gym.common.entity.BaseEntity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("food_preference")
public class FoodPreference extends BaseEntity {

    private Long userId;
    private String preferenceType;
    private Long foodId;
    private String preferenceKey;
    private String preferenceValue;
    private String source;
    private BigDecimal confidence;
}
