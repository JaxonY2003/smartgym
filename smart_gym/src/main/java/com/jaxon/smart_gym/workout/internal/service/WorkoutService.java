package com.jaxon.smart_gym.workout.internal.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jaxon.smart_gym.common.exception.BusinessException;
import com.jaxon.smart_gym.common.response.PageResponse;
import com.jaxon.smart_gym.training.internal.entity.Exercise;
import com.jaxon.smart_gym.training.internal.mapper.ExerciseMapper;
import com.jaxon.smart_gym.workout.internal.dto.request.WorkoutFeedbackRequest;
import com.jaxon.smart_gym.workout.internal.dto.request.WorkoutPlanCreateRequest;
import com.jaxon.smart_gym.workout.internal.dto.request.WorkoutPlanUpdateRequest;
import com.jaxon.smart_gym.workout.internal.dto.request.WorkoutSessionCreateRequest;
import com.jaxon.smart_gym.workout.internal.dto.response.WorkoutPlanResponse;
import com.jaxon.smart_gym.workout.internal.dto.response.WorkoutSessionResponse;
import com.jaxon.smart_gym.workout.internal.entity.WorkoutFeedback;
import com.jaxon.smart_gym.workout.internal.entity.WorkoutPlan;
import com.jaxon.smart_gym.workout.internal.entity.WorkoutPlanDay;
import com.jaxon.smart_gym.workout.internal.entity.WorkoutPlanExercise;
import com.jaxon.smart_gym.workout.internal.entity.WorkoutSession;
import com.jaxon.smart_gym.workout.internal.entity.WorkoutSetRecord;
import com.jaxon.smart_gym.workout.internal.mapper.WorkoutFeedbackMapper;
import com.jaxon.smart_gym.workout.internal.mapper.WorkoutPlanDayMapper;
import com.jaxon.smart_gym.workout.internal.mapper.WorkoutPlanExerciseMapper;
import com.jaxon.smart_gym.workout.internal.mapper.WorkoutPlanMapper;
import com.jaxon.smart_gym.workout.internal.mapper.WorkoutSessionMapper;
import com.jaxon.smart_gym.workout.internal.mapper.WorkoutSetRecordMapper;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Service
public class WorkoutService {

    private static final String MANUAL = "MANUAL";
    private static final String DRAFT = "DRAFT";
    private static final String ACTIVE = "ACTIVE";
    private static final String ARCHIVED = "ARCHIVED";
    private static final String IN_PROGRESS = "IN_PROGRESS";
    private static final String COMPLETED = "COMPLETED";
    private static final String CANCELLED = "CANCELLED";

    private final WorkoutPlanMapper workoutPlanMapper;
    private final WorkoutPlanDayMapper workoutPlanDayMapper;
    private final WorkoutPlanExerciseMapper workoutPlanExerciseMapper;
    private final WorkoutSessionMapper workoutSessionMapper;
    private final WorkoutSetRecordMapper workoutSetRecordMapper;
    private final WorkoutFeedbackMapper workoutFeedbackMapper;
    private final ExerciseMapper exerciseMapper;
    private final JsonMapper objectMapper;
    private final Clock clock;

    public WorkoutService(
            WorkoutPlanMapper workoutPlanMapper,
            WorkoutPlanDayMapper workoutPlanDayMapper,
            WorkoutPlanExerciseMapper workoutPlanExerciseMapper,
            WorkoutSessionMapper workoutSessionMapper,
            WorkoutSetRecordMapper workoutSetRecordMapper,
            WorkoutFeedbackMapper workoutFeedbackMapper,
            ExerciseMapper exerciseMapper,
            JsonMapper objectMapper,
            Clock clock) {
        this.workoutPlanMapper = workoutPlanMapper;
        this.workoutPlanDayMapper = workoutPlanDayMapper;
        this.workoutPlanExerciseMapper = workoutPlanExerciseMapper;
        this.workoutSessionMapper = workoutSessionMapper;
        this.workoutSetRecordMapper = workoutSetRecordMapper;
        this.workoutFeedbackMapper = workoutFeedbackMapper;
        this.exerciseMapper = exerciseMapper;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<WorkoutPlanResponse> listPlans(
            Long userId,
            String status,
            long page,
            long pageSize) {
        LambdaQueryWrapper<WorkoutPlan> query = new LambdaQueryWrapper<WorkoutPlan>()
                .eq(WorkoutPlan::getUserId, userId)
                .isNull(WorkoutPlan::getDeletedAt)
                .orderByDesc(WorkoutPlan::getUpdatedAt)
                .orderByDesc(WorkoutPlan::getId);
        String normalizedStatus = normalizePlanStatus(status, true);
        if (normalizedStatus != null) {
            query.eq(WorkoutPlan::getStatus, normalizedStatus);
        }

        Page<WorkoutPlan> result = workoutPlanMapper.selectPage(
                new Page<>(page, pageSize), query);
        List<WorkoutPlanResponse> items = result.getRecords().stream()
                .map(this::toPlanResponse)
                .toList();
        return PageResponse.of(items, result.getCurrent(), result.getSize(), result.getTotal());
    }

    @Transactional(readOnly = true)
    public WorkoutPlanResponse getPlan(Long userId, Long planId) {
        return toPlanResponse(requirePlan(userId, planId));
    }

    @Transactional(readOnly = true)
    public WorkoutPlanResponse getTodayPlan(Long userId, LocalDate date) {
        LocalDate targetDate = date == null ? LocalDate.now(clock) : date;
        LambdaQueryWrapper<WorkoutPlan> query = new LambdaQueryWrapper<WorkoutPlan>()
                .eq(WorkoutPlan::getUserId, userId)
                .eq(WorkoutPlan::getStatus, ACTIVE)
                .isNull(WorkoutPlan::getDeletedAt)
                .and(wrapper -> wrapper
                        .isNull(WorkoutPlan::getStartsOn)
                        .or()
                        .le(WorkoutPlan::getStartsOn, targetDate))
                .and(wrapper -> wrapper
                        .isNull(WorkoutPlan::getEndsOn)
                        .or()
                        .ge(WorkoutPlan::getEndsOn, targetDate))
                .orderByDesc(WorkoutPlan::getStartsOn)
                .orderByDesc(WorkoutPlan::getId);
        List<WorkoutPlan> plans = workoutPlanMapper.selectPage(new Page<>(1, 1), query).getRecords();
        return plans.isEmpty() ? null : toPlanResponse(plans.get(0));
    }

    @Transactional
    public WorkoutPlanResponse createPlan(Long userId, WorkoutPlanCreateRequest request) {
        validatePlanDates(request.startsOn(), request.endsOn());
        WorkoutPlan plan = new WorkoutPlan();
        plan.setUserId(userId);
        plan.setCreatedByUserId(userId);
        plan.setName(request.name().trim());
        plan.setSource(MANUAL);
        plan.setGoal(normalizeNullable(request.goal()));
        plan.setStatus(DRAFT);
        plan.setStartsOn(request.startsOn());
        plan.setEndsOn(request.endsOn());
        plan.setDurationWeeks(request.durationWeeks());
        plan.setTrainingDaysPerWeek(request.trainingDaysPerWeek());
        plan.setVersion(1);
        workoutPlanMapper.insert(plan);

        int dayNo = 1;
        for (WorkoutPlanCreateRequest.WorkoutPlanDayRequest dayRequest : request.days()) {
            WorkoutPlanDay day = new WorkoutPlanDay();
            day.setPlanId(plan.getId());
            day.setDayNo(dayNo++);
            day.setDayOfWeek(dayRequest.dayOfWeek());
            day.setTitle(dayRequest.title().trim());
            day.setEstimatedMinutes(dayRequest.estimatedMinutes());
            day.setNotes(normalizeNullable(dayRequest.notes()));
            workoutPlanDayMapper.insert(day);

            int sortOrder = 1;
            for (WorkoutPlanCreateRequest.WorkoutPlanExerciseRequest exerciseRequest : dayRequest.exercises()) {
                Exercise exercise = requireActiveExercise(exerciseRequest.exerciseId());
                validateRepRange(exerciseRequest.targetRepsMin(), exerciseRequest.targetRepsMax());
                WorkoutPlanExercise planExercise = new WorkoutPlanExercise();
                planExercise.setPlanDayId(day.getId());
                planExercise.setExerciseId(exercise.getId());
                planExercise.setSortOrder(sortOrder++);
                planExercise.setSets(defaultValue(exerciseRequest.sets(), 1));
                planExercise.setTargetRepsMin(exerciseRequest.targetRepsMin());
                planExercise.setTargetRepsMax(exerciseRequest.targetRepsMax());
                planExercise.setTargetWeightKg(exerciseRequest.targetWeightKg());
                planExercise.setRestSeconds(defaultValue(
                        exerciseRequest.restSeconds(), exercise.getDefaultRestSeconds()));
                planExercise.setTempo(normalizeNullable(exerciseRequest.tempo()));
                planExercise.setNotes(normalizeNullable(exerciseRequest.notes()));
                workoutPlanExerciseMapper.insert(planExercise);
            }
        }
        return getPlan(userId, plan.getId());
    }

    @Transactional
    public WorkoutPlanResponse updatePlan(
            Long userId,
            Long planId,
            WorkoutPlanUpdateRequest request) {
        lockPlanOwner(userId);
        WorkoutPlan plan = requirePlan(userId, planId);
        if (request.name() != null) {
            if (request.name().isBlank()) {
                throw new BusinessException("VALIDATION_ERROR", "计划名称不能为空", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            plan.setName(request.name().trim());
        }
        if (request.goal() != null) {
            plan.setGoal(normalizeNullable(request.goal()));
        }
        if (request.status() != null) {
            plan.setStatus(normalizePlanStatus(request.status(), false));
        }
        if (request.startsOn() != null || request.endsOn() != null) {
            LocalDate startsOn = request.startsOn() == null ? plan.getStartsOn() : request.startsOn();
            LocalDate endsOn = request.endsOn() == null ? plan.getEndsOn() : request.endsOn();
            validatePlanDates(startsOn, endsOn);
            plan.setStartsOn(startsOn);
            plan.setEndsOn(endsOn);
        }
        if (request.durationWeeks() != null) {
            plan.setDurationWeeks(request.durationWeeks());
        }
        if (request.trainingDaysPerWeek() != null) {
            plan.setTrainingDaysPerWeek(request.trainingDaysPerWeek());
        }
        if (ACTIVE.equals(plan.getStatus())) {
            workoutPlanMapper.pauseOtherActivePlans(userId, planId);
        }
        plan.setVersion(defaultValue(plan.getVersion(), 1) + 1);
        workoutPlanMapper.updateById(plan);
        return getPlan(userId, planId);
    }

    @Transactional
    public WorkoutPlanResponse activatePlan(Long userId, Long planId) {
        lockPlanOwner(userId);
        WorkoutPlan plan = requirePlan(userId, planId);
        workoutPlanMapper.pauseOtherActivePlans(userId, planId);
        plan.setStatus(ACTIVE);
        plan.setVersion(defaultValue(plan.getVersion(), 1) + 1);
        workoutPlanMapper.updateById(plan);
        return getPlan(userId, planId);
    }

    @Transactional
    public void deletePlan(Long userId, Long planId) {
        lockPlanOwner(userId);
        requirePlan(userId, planId);
        workoutPlanMapper.update(
                null,
                new LambdaUpdateWrapper<WorkoutPlan>()
                        .eq(WorkoutPlan::getId, planId)
                        .eq(WorkoutPlan::getUserId, userId)
                        .isNull(WorkoutPlan::getDeletedAt)
                        .set(WorkoutPlan::getStatus, ARCHIVED)
                        .set(WorkoutPlan::getDeletedAt, LocalDateTime.now(clock)));
    }

    @Transactional
    public WorkoutSessionResponse createSession(Long userId, WorkoutSessionCreateRequest request) {
        Long planId = request.planId();
        Long planDayId = request.planDayId();
        if (planDayId != null) {
            WorkoutPlanDay day = workoutPlanDayMapper.selectById(planDayId);
            if (day == null) {
                throw notFound("PLAN_DAY_NOT_FOUND", "训练日不存在");
            }
            if (planId != null && !planId.equals(day.getPlanId())) {
                throw conflict("PLAN_DAY_MISMATCH", "训练日不属于指定训练计划");
            }
            planId = day.getPlanId();
        }
        if (planId != null) {
            requirePlan(userId, planId);
        }

        LocalDateTime startedAt = toUtc(request.startedAt());
        LocalDateTime endedAt = toUtc(request.endedAt());
        if (startedAt != null && endedAt != null && endedAt.isBefore(startedAt)) {
            throw new BusinessException("VALIDATION_ERROR", "结束时间不能早于开始时间", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        String status = normalizeSessionStatus(request.status(), endedAt != null);
        Set<String> setKeys = new HashSet<>();
        BigDecimal totalVolume = BigDecimal.ZERO;
        for (WorkoutSessionCreateRequest.WorkoutSetRequest setRequest : request.sets()) {
            requireActiveExercise(setRequest.exerciseId());
            String key = setRequest.exerciseId() + ":" + setRequest.setIndex();
            if (!setKeys.add(key)) {
                throw conflict("DUPLICATE_SET_INDEX", "同一动作的组序号不能重复");
            }
            if (Boolean.TRUE.equals(setRequest.completed())
                    && setRequest.weightKg() != null
                    && setRequest.reps() != null) {
                totalVolume = totalVolume.add(setRequest.weightKg()
                        .multiply(BigDecimal.valueOf(setRequest.reps())));
            }
        }

        WorkoutSession session = new WorkoutSession();
        session.setUserId(userId);
        session.setPlanId(planId);
        session.setPlanDayId(planDayId);
        session.setStatus(status);
        session.setStartedAt(startedAt);
        session.setEndedAt(endedAt);
        session.setFatigueLevel(request.fatigueLevel());
        session.setPainBodyPart(normalizeNullable(request.painBodyPart()));
        session.setPainLevel(request.painLevel());
        session.setTotalVolumeKg(totalVolume);
        session.setNote(normalizeNullable(request.note()));
        workoutSessionMapper.insert(session);

        for (WorkoutSessionCreateRequest.WorkoutSetRequest setRequest : request.sets()) {
            WorkoutSetRecord set = new WorkoutSetRecord();
            set.setSessionId(session.getId());
            set.setExerciseId(setRequest.exerciseId());
            set.setSetIndex(setRequest.setIndex());
            set.setWeightKg(setRequest.weightKg());
            set.setReps(setRequest.reps());
            set.setCompleted(Boolean.TRUE.equals(setRequest.completed()));
            set.setRestSeconds(setRequest.restSeconds());
            set.setRpe(setRequest.rpe());
            set.setNote(normalizeNullable(setRequest.note()));
            workoutSetRecordMapper.insert(set);
        }
        return getSession(userId, session.getId());
    }

    @Transactional(readOnly = true)
    public PageResponse<WorkoutSessionResponse> listSessions(
            Long userId,
            LocalDate from,
            LocalDate to,
            String status,
            long page,
            long pageSize) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException("VALIDATION_ERROR", "开始日期不能晚于结束日期", HttpStatus.BAD_REQUEST);
        }
        LambdaQueryWrapper<WorkoutSession> query = new LambdaQueryWrapper<WorkoutSession>()
                .eq(WorkoutSession::getUserId, userId)
                .orderByDesc(WorkoutSession::getStartedAt)
                .orderByDesc(WorkoutSession::getId);
        if (from != null) {
            query.ge(WorkoutSession::getStartedAt, from.atStartOfDay());
        }
        if (to != null) {
            query.lt(WorkoutSession::getStartedAt, to.plusDays(1).atStartOfDay());
        }
        if (status != null && !status.isBlank()) {
            query.eq(WorkoutSession::getStatus, normalizeSessionStatus(status, false));
        }

        Page<WorkoutSession> result = workoutSessionMapper.selectPage(
                new Page<>(page, pageSize), query);
        List<WorkoutSessionResponse> items = result.getRecords().stream()
                .map(this::toSessionResponse)
                .toList();
        return PageResponse.of(items, result.getCurrent(), result.getSize(), result.getTotal());
    }

    @Transactional(readOnly = true)
    public WorkoutSessionResponse getSession(Long userId, Long sessionId) {
        WorkoutSession session = requireSession(userId, sessionId);
        return toSessionResponse(session);
    }

    @Transactional
    public WorkoutSessionResponse addFeedback(
            Long userId,
            Long sessionId,
            WorkoutFeedbackRequest request) {
        WorkoutSession session = requireSession(userId, sessionId);
        WorkoutFeedback feedback = new WorkoutFeedback();
        feedback.setSessionId(session.getId());
        feedback.setUserId(userId);
        feedback.setFatigueLevel(request.fatigueLevel());
        feedback.setPainBodyPart(normalizeNullable(request.painBodyPart()));
        feedback.setPainLevel(request.painLevel());
        feedback.setFeeling(normalizeNullable(request.feeling()));
        feedback.setShouldAdjustNextPlan(Boolean.TRUE.equals(request.shouldAdjustNextPlan()));
        workoutFeedbackMapper.insert(feedback);
        return getSession(userId, sessionId);
    }

    private void lockPlanOwner(Long userId) {
        if (workoutPlanMapper.lockOwnerForPlanChange(userId) == null) {
            throw new BusinessException("AUTH_TOKEN_INVALID", "登录状态无效，请重新登录", HttpStatus.UNAUTHORIZED);
        }
    }

    private WorkoutPlan requirePlan(Long userId, Long planId) {
        WorkoutPlan plan = workoutPlanMapper.selectOne(
                new LambdaQueryWrapper<WorkoutPlan>()
                        .eq(WorkoutPlan::getId, planId)
                        .eq(WorkoutPlan::getUserId, userId)
                        .isNull(WorkoutPlan::getDeletedAt));
        if (plan == null) {
            throw notFound("PLAN_NOT_FOUND", "训练计划不存在");
        }
        return plan;
    }

    private WorkoutSession requireSession(Long userId, Long sessionId) {
        WorkoutSession session = workoutSessionMapper.selectOne(
                new LambdaQueryWrapper<WorkoutSession>()
                        .eq(WorkoutSession::getId, sessionId)
                        .eq(WorkoutSession::getUserId, userId));
        if (session == null) {
            throw notFound("SESSION_NOT_FOUND", "训练记录不存在");
        }
        return session;
    }

    private Exercise requireActiveExercise(Long exerciseId) {
        Exercise exercise = exerciseMapper.selectOne(
                new LambdaQueryWrapper<Exercise>()
                        .eq(Exercise::getId, exerciseId)
                        .eq(Exercise::getStatus, "ACTIVE"));
        if (exercise == null) {
            throw notFound("EXERCISE_NOT_FOUND", "训练动作不存在");
        }
        return exercise;
    }

    private WorkoutPlanResponse toPlanResponse(WorkoutPlan plan) {
        List<WorkoutPlanDay> days = workoutPlanDayMapper.selectList(
                new LambdaQueryWrapper<WorkoutPlanDay>()
                        .eq(WorkoutPlanDay::getPlanId, plan.getId())
                        .orderByAsc(WorkoutPlanDay::getDayNo));
        List<WorkoutPlanResponse.WorkoutPlanDayResponse> dayResponses = days.stream()
                .map(this::toPlanDayResponse)
                .toList();
        return new WorkoutPlanResponse(
                plan.getId(),
                plan.getName(),
                plan.getSource(),
                plan.getStatus(),
                plan.getGoal(),
                plan.getStartsOn(),
                plan.getEndsOn(),
                plan.getDurationWeeks(),
                plan.getTrainingDaysPerWeek(),
                plan.getVersion(),
                parseJsonList(plan.getWarningsJson()),
                parseJsonList(plan.getAssumptionsJson()),
                dayResponses,
                toUtc(plan.getCreatedAt()),
                toUtc(plan.getUpdatedAt()));
    }

    private WorkoutPlanResponse.WorkoutPlanDayResponse toPlanDayResponse(WorkoutPlanDay day) {
        List<WorkoutPlanExercise> exercises = workoutPlanExerciseMapper.selectList(
                new LambdaQueryWrapper<WorkoutPlanExercise>()
                        .eq(WorkoutPlanExercise::getPlanDayId, day.getId())
                        .orderByAsc(WorkoutPlanExercise::getSortOrder)
                        .orderByAsc(WorkoutPlanExercise::getId));
        Map<Long, String> exerciseNames = loadExerciseNames(
                exercises.stream().map(WorkoutPlanExercise::getExerciseId).toList());
        List<WorkoutPlanResponse.WorkoutPlanExerciseResponse> exerciseResponses = exercises.stream()
                .map(item -> new WorkoutPlanResponse.WorkoutPlanExerciseResponse(
                        item.getId(),
                        item.getExerciseId(),
                        exerciseNames.get(item.getExerciseId()),
                        item.getSortOrder(),
                        item.getSets(),
                        item.getTargetRepsMin(),
                        item.getTargetRepsMax(),
                        item.getTargetWeightKg(),
                        item.getRestSeconds(),
                        item.getTempo(),
                        item.getNotes()))
                .toList();
        return new WorkoutPlanResponse.WorkoutPlanDayResponse(
                day.getId(),
                day.getDayNo(),
                day.getDayOfWeek(),
                day.getTitle(),
                day.getEstimatedMinutes(),
                day.getNotes(),
                exerciseResponses);
    }

    private WorkoutSessionResponse toSessionResponse(WorkoutSession session) {
        List<WorkoutSetRecord> sets = workoutSetRecordMapper.selectList(
                new LambdaQueryWrapper<WorkoutSetRecord>()
                        .eq(WorkoutSetRecord::getSessionId, session.getId())
                        .orderByAsc(WorkoutSetRecord::getExerciseId)
                        .orderByAsc(WorkoutSetRecord::getSetIndex));
        Map<Long, String> exerciseNames = loadExerciseNames(
                sets.stream().map(WorkoutSetRecord::getExerciseId).toList());
        List<WorkoutSessionResponse.WorkoutSetResponse> setResponses = sets.stream()
                .map(set -> new WorkoutSessionResponse.WorkoutSetResponse(
                        set.getId(),
                        set.getExerciseId(),
                        exerciseNames.get(set.getExerciseId()),
                        set.getSetIndex(),
                        set.getWeightKg(),
                        set.getReps(),
                        set.getCompleted(),
                        set.getRestSeconds(),
                        set.getRpe(),
                        set.getNote()))
                .toList();
        List<WorkoutFeedback> feedbacks = workoutFeedbackMapper.selectList(
                new LambdaQueryWrapper<WorkoutFeedback>()
                        .eq(WorkoutFeedback::getSessionId, session.getId())
                        .orderByDesc(WorkoutFeedback::getCreatedAt));
        List<WorkoutSessionResponse.WorkoutFeedbackResponse> feedbackResponses = feedbacks.stream()
                .map(feedback -> new WorkoutSessionResponse.WorkoutFeedbackResponse(
                        feedback.getId(),
                        feedback.getFatigueLevel(),
                        feedback.getPainBodyPart(),
                        feedback.getPainLevel(),
                        feedback.getFeeling(),
                        feedback.getShouldAdjustNextPlan(),
                        toUtc(feedback.getCreatedAt()),
                        toUtc(feedback.getUpdatedAt())))
                .toList();
        Long durationMinutes = null;
        if (session.getStartedAt() != null && session.getEndedAt() != null) {
            durationMinutes = ChronoUnit.MINUTES.between(session.getStartedAt(), session.getEndedAt());
        }
        return new WorkoutSessionResponse(
                session.getId(),
                session.getPlanId(),
                session.getPlanDayId(),
                session.getStatus(),
                toUtc(session.getStartedAt()),
                toUtc(session.getEndedAt()),
                durationMinutes,
                session.getTotalVolumeKg(),
                session.getFatigueLevel(),
                session.getPainBodyPart(),
                session.getPainLevel(),
                session.getNote(),
                setResponses,
                feedbackResponses,
                toUtc(session.getCreatedAt()),
                toUtc(session.getUpdatedAt()));
    }

    private Map<Long, String> loadExerciseNames(List<Long> ids) {
        List<Long> distinctIds = ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (distinctIds.isEmpty()) {
            return Map.of();
        }
        return exerciseMapper.selectByIds(distinctIds).stream()
                .collect(java.util.stream.Collectors.toMap(Exercise::getId, Exercise::getName));
    }

    private List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() { });
        } catch (RuntimeException exception) {
            return Collections.emptyList();
        }
    }

    private String normalizePlanStatus(String status, boolean nullable) {
        if (status == null || status.isBlank()) {
            return nullable ? null : DRAFT;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!Set.of(DRAFT, ACTIVE, "PAUSED", COMPLETED, ARCHIVED).contains(normalized)) {
            throw new BusinessException("VALIDATION_ERROR", "训练计划状态不合法", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private String normalizeSessionStatus(String status, boolean ended) {
        if (status == null || status.isBlank()) {
            return ended ? COMPLETED : IN_PROGRESS;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!Set.of(IN_PROGRESS, COMPLETED, CANCELLED).contains(normalized)) {
            throw new BusinessException("VALIDATION_ERROR", "训练记录状态不合法", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private void validatePlanDates(LocalDate startsOn, LocalDate endsOn) {
        if (startsOn != null && endsOn != null && endsOn.isBefore(startsOn)) {
            throw new BusinessException("VALIDATION_ERROR", "计划结束日期不能早于开始日期", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private void validateRepRange(Integer min, Integer max) {
        if (min != null && max != null && min > max) {
            throw new BusinessException("VALIDATION_ERROR", "目标次数下限不能大于上限", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private <T> T defaultValue(T value, T fallback) {
        return value == null ? fallback : value;
    }

    private LocalDateTime toUtc(OffsetDateTime value) {
        return value == null ? null : value.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    private OffsetDateTime toUtc(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }

    private BusinessException notFound(String code, String message) {
        return new BusinessException(code, message, HttpStatus.NOT_FOUND);
    }

    private BusinessException conflict(String code, String message) {
        return new BusinessException(code, message, HttpStatus.CONFLICT);
    }
}
