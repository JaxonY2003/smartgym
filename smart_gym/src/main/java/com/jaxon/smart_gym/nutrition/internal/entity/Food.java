package com.jaxon.smart_gym.nutrition.internal.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jaxon.smart_gym.common.entity.BaseEntity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("food")
public class Food extends BaseEntity {

    private Long categoryId;
    private String name;
    private String englishName;
    private String description;
    private String status;
    private String source;
    private String sourceVersion;
}
