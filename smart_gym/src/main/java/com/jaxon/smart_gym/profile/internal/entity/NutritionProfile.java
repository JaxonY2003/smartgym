package com.jaxon.smart_gym.profile.internal.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jaxon.smart_gym.common.entity.BaseEntity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("nutrition_profile")
public class NutritionProfile extends BaseEntity {

    private Long userId;
    private String dietType;
    private Integer dailyWaterTargetMl;
    private String note;
}
