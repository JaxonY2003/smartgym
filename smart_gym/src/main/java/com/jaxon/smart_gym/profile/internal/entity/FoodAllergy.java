package com.jaxon.smart_gym.profile.internal.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jaxon.smart_gym.common.entity.BaseEntity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("food_allergy")
public class FoodAllergy extends BaseEntity {

    private Long userId;
    private Long allergenId;
    private String severity;
    private String source;
    private Boolean active;
    private String note;
}
