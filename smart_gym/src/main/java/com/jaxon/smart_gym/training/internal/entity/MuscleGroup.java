package com.jaxon.smart_gym.training.internal.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jaxon.smart_gym.common.entity.BaseEntity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("muscle_group")
public class MuscleGroup extends BaseEntity {

    private String code;
    private String name;
    private Long parentId;
    private Integer sortOrder;
    private String status;
}
