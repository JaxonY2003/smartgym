package com.jaxon.smart_gym.profile.internal.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jaxon.smart_gym.common.entity.BaseEntity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("training_goal")
public class TrainingGoal extends BaseEntity {

    private Long userId;
    private String goalType;
    private Integer priority;
    private BigDecimal targetWeightKg;
    private BigDecimal targetBodyFatPercent;
    private LocalDate targetDate;
    private String status;
    private String note;
}
