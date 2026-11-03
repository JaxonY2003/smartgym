package com.jaxon.smart_gym.workout.internal.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jaxon.smart_gym.common.entity.BaseEntity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("workout_plan_day")
public class WorkoutPlanDay extends BaseEntity {

    private Long planId;
    private Integer dayNo;
    private Integer dayOfWeek;
    private String title;
    private Integer estimatedMinutes;
    private String notes;
}
