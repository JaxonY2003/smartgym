package com.jaxon.smart_gym.workout.internal.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jaxon.smart_gym.common.entity.BaseEntity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("workout_session")
public class WorkoutSession extends BaseEntity {

    private Long userId;
    private Long planId;
    private Long planDayId;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Integer fatigueLevel;
    private String painBodyPart;
    private Integer painLevel;
    private BigDecimal totalVolumeKg;
    private String note;
}
