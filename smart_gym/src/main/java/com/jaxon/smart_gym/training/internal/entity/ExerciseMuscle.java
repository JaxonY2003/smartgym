package com.jaxon.smart_gym.training.internal.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jaxon.smart_gym.common.entity.BaseEntity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("exercise_muscle")
public class ExerciseMuscle extends BaseEntity {

    private Long exerciseId;
    private Long muscleGroupId;
    private String muscleRole;
}
