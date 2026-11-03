package com.jaxon.smart_gym.nutrition.internal.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jaxon.smart_gym.common.entity.BaseEntity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("meal_record")
public class MealRecord extends BaseEntity {

    private Long userId;
    private LocalDateTime eatenAt;
    private String mealType;
    private String inputSource;
    private BigDecimal totalCalories;
    private BigDecimal totalProteinG;
    private BigDecimal totalCarbohydrateG;
    private BigDecimal totalFatG;
    private BigDecimal totalFiberG;
    private String note;
    private Boolean confirmedByUser;
}
