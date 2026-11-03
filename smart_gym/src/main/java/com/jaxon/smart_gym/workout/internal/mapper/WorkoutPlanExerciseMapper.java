package com.jaxon.smart_gym.workout.internal.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jaxon.smart_gym.workout.internal.entity.WorkoutPlanExercise;

@Mapper
public interface WorkoutPlanExerciseMapper extends BaseMapper<WorkoutPlanExercise> {
}
