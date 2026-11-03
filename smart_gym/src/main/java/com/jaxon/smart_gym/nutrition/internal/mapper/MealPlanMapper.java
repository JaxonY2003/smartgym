package com.jaxon.smart_gym.nutrition.internal.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jaxon.smart_gym.nutrition.internal.entity.MealPlan;

@Mapper
public interface MealPlanMapper extends BaseMapper<MealPlan> {
}
