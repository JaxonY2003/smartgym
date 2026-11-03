package com.jaxon.smart_gym.profile.internal.entity;

import java.time.LocalDate;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jaxon.smart_gym.common.entity.BaseEntity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("health_constraint")
public class HealthConstraint extends BaseEntity {

    private Long userId;
    private String constraintType;
    private String bodyPart;
    private String severity;
    private String description;
    private Boolean active;
    private LocalDate startedOn;
    private LocalDate endedOn;
}
