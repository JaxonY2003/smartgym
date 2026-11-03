package com.jaxon.smart_gym.workout.internal.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jaxon.smart_gym.workout.internal.entity.WorkoutSetRecord;

@Mapper
public interface WorkoutSetRecordMapper extends BaseMapper<WorkoutSetRecord> {
}
