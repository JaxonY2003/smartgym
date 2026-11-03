package com.jaxon.smart_gym.workout.internal.entity;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jaxon.smart_gym.common.entity.BaseEntity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("workout_set_record")
public class WorkoutSetRecord extends BaseEntity {

    private Long sessionId;
    private Long exerciseId;
    private Integer setIndex;
    private BigDecimal weightKg;
    private Integer reps;
    private Boolean completed;
    private Integer restSeconds;
    private BigDecimal rpe;
    private String note;
}
