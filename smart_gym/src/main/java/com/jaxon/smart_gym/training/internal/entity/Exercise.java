package com.jaxon.smart_gym.training.internal.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jaxon.smart_gym.common.entity.BaseEntity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("exercise")
public class Exercise extends BaseEntity {

    private String name;
    private String englishName;
    private String description;
    private String difficulty;
    private Integer defaultRestSeconds;
    private String instructions;
    private String contraindicationsJson;
    private String thumbnailUrl;
    private String status;
}
