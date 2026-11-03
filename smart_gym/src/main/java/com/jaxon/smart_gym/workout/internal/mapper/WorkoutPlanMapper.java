package com.jaxon.smart_gym.workout.internal.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jaxon.smart_gym.workout.internal.entity.WorkoutPlan;

@Mapper
public interface WorkoutPlanMapper extends BaseMapper<WorkoutPlan> {

    // A stable per-user lock also serializes activations when there are no ACTIVE plans yet.
    // Call inside the same transaction, before reading or changing the user's plans.
    @Select("SELECT id FROM sys_user WHERE id = #{userId} AND deleted_at IS NULL FOR UPDATE")
    Long lockOwnerForPlanChange(@Param("userId") Long userId);

    @Update("""
            UPDATE workout_plan
            SET status = 'PAUSED', version = version + 1
            WHERE user_id = #{userId}
              AND status = 'ACTIVE'
              AND id <> #{planId}
              AND deleted_at IS NULL
            """)
    int pauseOtherActivePlans(@Param("userId") Long userId, @Param("planId") Long planId);
}
