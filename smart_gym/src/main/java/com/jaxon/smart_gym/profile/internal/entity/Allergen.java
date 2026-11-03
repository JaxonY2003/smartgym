package com.jaxon.smart_gym.profile.internal.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jaxon.smart_gym.common.entity.BaseEntity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("allergen")
public class Allergen extends BaseEntity {

    private String code;
    private String name;
    private String description;
}
