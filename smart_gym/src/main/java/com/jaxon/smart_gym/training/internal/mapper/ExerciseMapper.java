package com.jaxon.smart_gym.training.internal.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jaxon.smart_gym.training.internal.entity.Exercise;

@Mapper
public interface ExerciseMapper extends BaseMapper<Exercise> {
}
