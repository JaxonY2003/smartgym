package com.jaxon.smart_gym.workout.internal.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jaxon.smart_gym.common.entity.BaseEntity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("workout_feedback")
public class WorkoutFeedback extends BaseEntity {

    private Long sessionId;
    private Long userId;
    private Integer fatigueLevel;
    private String painBodyPart;
    private Integer painLevel;
    private String feeling;
    private Boolean shouldAdjustNextPlan;
}
