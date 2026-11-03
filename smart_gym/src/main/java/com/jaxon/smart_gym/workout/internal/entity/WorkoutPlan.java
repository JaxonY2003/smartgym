package com.jaxon.smart_gym.workout.internal.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jaxon.smart_gym.common.entity.BaseEntity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("workout_plan")
public class WorkoutPlan extends BaseEntity {

    private Long userId;
    private String name;
    private String source;
    private String goal;
    private String status;
    private LocalDate startsOn;
    private LocalDate endsOn;
    private Integer durationWeeks;
    private Integer trainingDaysPerWeek;
    private String warningsJson;
    private String assumptionsJson;
    private String confirmedRunId;
    private Integer version;
    private Long createdByUserId;
    private LocalDateTime deletedAt;
}
