package com.jaxon.smart_gym.workout.internal.mapper;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.baomidou.mybatisplus.core.MybatisConfiguration;

class WorkoutPlanMapperTests {

    private MybatisConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new MybatisConfiguration();
        configuration.addMapper(WorkoutPlanMapper.class);
    }

    @Test
    void ownerLockUsesStableUserRowEvenWhenUserHasNoActivePlans() {
        var sql = configuration.getMappedStatement(WorkoutPlanMapper.class.getName() + ".lockOwnerForPlanChange")
                .getBoundSql(Map.of("userId", 1L));
        assertEquals("SELECT id FROM sys_user WHERE id = ? AND deleted_at IS NULL FOR UPDATE", sql.getSql());
        assertEquals("userId", sql.getParameterMappings().getFirst().getProperty());
    }

    @Test
    void pauseSqlScopesOwnerExcludesTargetAndDeletedPlansAndIncrementsVersion() {
        var sql = configuration.getMappedStatement(WorkoutPlanMapper.class.getName() + ".pauseOtherActivePlans")
                .getBoundSql(Map.of("userId", 1L, "planId", 3L));
        assertEquals("UPDATE workout_plan SET status = 'PAUSED', version = version + 1 "
                        + "WHERE user_id = ? AND status = 'ACTIVE' AND id <> ? AND deleted_at IS NULL",
                sql.getSql().replaceAll("\\s+", " ").trim());
        assertEquals("userId", sql.getParameterMappings().get(0).getProperty());
        assertEquals("planId", sql.getParameterMappings().get(1).getProperty());
    }
}
