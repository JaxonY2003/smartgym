package com.jaxon.smart_gym.profile.internal.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jaxon.smart_gym.common.entity.BaseEntity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("body_metric")
public class BodyMetric extends BaseEntity {

    private Long userId;
    private LocalDateTime measuredAt;
    private BigDecimal weightKg;
    private BigDecimal bodyFatPercent;
    private BigDecimal waistCm;
    private String note;
}
