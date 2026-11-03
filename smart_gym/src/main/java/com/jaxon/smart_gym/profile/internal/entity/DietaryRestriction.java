package com.jaxon.smart_gym.profile.internal.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jaxon.smart_gym.common.entity.BaseEntity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("dietary_restriction")
public class DietaryRestriction extends BaseEntity {

    private Long userId;
    private String restrictionCode;
    private String description;
    private Boolean active;
}
