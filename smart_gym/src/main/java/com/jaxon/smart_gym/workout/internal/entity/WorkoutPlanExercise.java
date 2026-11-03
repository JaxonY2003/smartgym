package com.jaxon.smart_gym.workout.internal.entity;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jaxon.smart_gym.common.entity.BaseEntity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("workout_plan_exercise")
public class WorkoutPlanExercise extends BaseEntity {

    private Long planDayId;
    private Long exerciseId;
    private Integer sortOrder;
    private Integer sets;
    private Integer targetRepsMin;
    private Integer targetRepsMax;
    private BigDecimal targetWeightKg;
    private Integer restSeconds;
    private String tempo;
    private String notes;
}
