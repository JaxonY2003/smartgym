package com.jaxon.smart_gym.workout.internal.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.jaxon.smart_gym.common.exception.BusinessException;
import com.jaxon.smart_gym.training.internal.mapper.ExerciseMapper;
import com.jaxon.smart_gym.workout.internal.dto.request.WorkoutPlanUpdateRequest;
import com.jaxon.smart_gym.workout.internal.entity.WorkoutPlan;
import com.jaxon.smart_gym.workout.internal.mapper.*;

import tools.jackson.databind.json.JsonMapper;

class WorkoutPlanActivationTests {

    private WorkoutPlanMapper plans;
    private WorkoutPlanDayMapper days;
    private WorkoutService service;
    private WorkoutPlan target;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"),
                WorkoutPlan.class);
        plans = mock(WorkoutPlanMapper.class);
        days = mock(WorkoutPlanDayMapper.class);
        service = new WorkoutService(plans, days, mock(WorkoutPlanExerciseMapper.class),
                mock(WorkoutSessionMapper.class), mock(WorkoutSetRecordMapper.class),
                mock(WorkoutFeedbackMapper.class), mock(ExerciseMapper.class),
                JsonMapper.builder().build(), Clock.systemUTC());
        target = new WorkoutPlan();
        target.setId(3L);
        target.setUserId(1L);
        target.setName("减脂力量训练计划");
        target.setStatus("PAUSED");
        target.setVersion(4);
        when(plans.lockOwnerForPlanChange(1L)).thenReturn(1L);
        when(plans.selectOne(any())).thenReturn(target);
        when(days.selectList(any())).thenReturn(List.of());
    }

    @Test
    void activationLocksOwnerThenPausesOthersThenActivatesAndIncrementsVersion() {
        var result = service.activatePlan(1L, 3L);

        InOrder order = inOrder(plans);
        order.verify(plans).lockOwnerForPlanChange(1L);
        order.verify(plans).selectOne(any());
        order.verify(plans).pauseOtherActivePlans(1L, 3L);
        order.verify(plans).updateById(target);
        assertEquals("ACTIVE", result.status());
        assertEquals(5, result.version());
    }

    @Test
    void patchResumeUsesSameMutualExclusion() {
        var result = service.updatePlan(1L, 3L, patch("ACTIVE"));

        InOrder order = inOrder(plans);
        order.verify(plans).lockOwnerForPlanChange(1L);
        order.verify(plans).selectOne(any());
        order.verify(plans).pauseOtherActivePlans(1L, 3L);
        order.verify(plans).updateById(target);
        assertEquals("ACTIVE", result.status());
        assertEquals(5, result.version());
    }

    @Test
    void patchPauseDoesNotChangeOtherPlans() {
        target.setStatus("ACTIVE");
        var result = service.updatePlan(1L, 3L, patch("PAUSED"));

        verify(plans).lockOwnerForPlanChange(1L);
        verify(plans, never()).pauseOtherActivePlans(anyLong(), anyLong());
        assertEquals("PAUSED", result.status());
        assertEquals(5, result.version());
    }

    @Test
    void editingActivePlanAlsoRepairsExistingMultipleActiveState() {
        target.setStatus("ACTIVE");
        service.updatePlan(1L, 3L, patch(null));
        verify(plans).pauseOtherActivePlans(1L, 3L);
    }

    @Test
    void repeatedActivationKeepsOneActiveAndDoesNotResetVersion() {
        service.activatePlan(1L, 3L);
        var result = service.activatePlan(1L, 3L);
        verify(plans, times(2)).pauseOtherActivePlans(1L, 3L);
        assertEquals("ACTIVE", result.status());
        assertEquals(6, result.version());
    }

    @Test
    void missingForeignOrDeletedTargetDoesNotPauseUsersCurrentPlan() {
        when(plans.selectOne(any())).thenReturn(null);
        BusinessException error = assertThrows(BusinessException.class,
                () -> service.activatePlan(1L, 999L));
        assertEquals("PLAN_NOT_FOUND", error.getCode());
        verify(plans, never()).pauseOtherActivePlans(anyLong(), anyLong());
        verify(plans, never()).updateById(any(WorkoutPlan.class));
    }

    @Test
    void invalidPatchDoesNotPauseOthers() {
        var request = new WorkoutPlanUpdateRequest(null, null, "ACTIVE",
                LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 1), null, null);
        assertThrows(BusinessException.class, () -> service.updatePlan(1L, 3L, request));
        verify(plans, never()).pauseOtherActivePlans(anyLong(), anyLong());
        verify(plans, never()).updateById(any(WorkoutPlan.class));
    }

    @Test
    void invalidOwnerStopsBeforeAnyPlanReadOrWrite() {
        when(plans.lockOwnerForPlanChange(1L)).thenReturn(null);
        BusinessException error = assertThrows(BusinessException.class,
                () -> service.activatePlan(1L, 3L));
        assertEquals("AUTH_TOKEN_INVALID", error.getCode());
        verify(plans, never()).selectOne(any());
        verify(plans, never()).pauseOtherActivePlans(anyLong(), anyLong());
    }

    @Test
    void deletionUsesSameOwnerLockBeforeReadingPlan() {
        service.deletePlan(1L, 3L);
        InOrder order = inOrder(plans);
        order.verify(plans).lockOwnerForPlanChange(1L);
        order.verify(plans).selectOne(any());
        order.verify(plans).update(isNull(), any(Wrapper.class));
    }

    private WorkoutPlanUpdateRequest patch(String status) {
        return new WorkoutPlanUpdateRequest(null, null, status, null, null, null, null);
    }
}
