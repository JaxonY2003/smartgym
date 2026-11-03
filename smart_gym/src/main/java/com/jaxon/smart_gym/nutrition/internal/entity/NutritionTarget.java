package com.jaxon.smart_gym.nutrition.internal.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jaxon.smart_gym.common.entity.BaseEntity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("nutrition_target")
public class NutritionTarget extends BaseEntity {

    private Long userId;
    private LocalDate effectiveDate;
    private String goal;
    private BigDecimal calories;
    private BigDecimal proteinG;
    private BigDecimal carbohydrateG;
    private BigDecimal fatG;
    private BigDecimal fiberG;
    private String source;
}
