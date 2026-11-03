package com.jaxon.smart_gym.profile.internal.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jaxon.smart_gym.common.entity.BaseEntity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("member_profile")
public class MemberProfile extends BaseEntity {

    private Long userId;
    private String gender;
    private LocalDate birthDate;
    private BigDecimal heightCm;
    private BigDecimal weightKg;
    private String trainingExperience;
    private Integer weeklyTrainingDays;
    private Integer maxSessionMinutes;
    private String medicalNotice;
    private String note;
}
