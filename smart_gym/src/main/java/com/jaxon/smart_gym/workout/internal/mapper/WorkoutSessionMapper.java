package com.jaxon.smart_gym.workout.internal.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jaxon.smart_gym.workout.internal.entity.WorkoutSession;

@Mapper
public interface WorkoutSessionMapper extends BaseMapper<WorkoutSession> {
}
