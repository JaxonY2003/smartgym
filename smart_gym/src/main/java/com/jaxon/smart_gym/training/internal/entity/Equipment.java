package com.jaxon.smart_gym.training.internal.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jaxon.smart_gym.common.entity.BaseEntity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("equipment")
public class Equipment extends BaseEntity {

    private String name;
    private String category;
    private String brand;
    private String status;
    private String description;
}
