package com.jaxon.smart_gym.dashboard.internal.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jaxon.smart_gym.common.exception.BusinessException;
import com.jaxon.smart_gym.dashboard.internal.dto.response.DashboardOverviewResponse;
import com.jaxon.smart_gym.dashboard.internal.dto.response.DashboardTrendResponse;
import com.jaxon.smart_gym.nutrition.internal.entity.DailyNutritionSummary;
import com.jaxon.smart_gym.nutrition.internal.entity.MealRecord;
import com.jaxon.smart_gym.nutrition.internal.entity.NutritionTarget;
import com.jaxon.smart_gym.nutrition.internal.mapper.DailyNutritionSummaryMapper;
import com.jaxon.smart_gym.nutrition.internal.mapper.MealRecordMapper;
import com.jaxon.smart_gym.nutrition.internal.mapper.NutritionTargetMapper;
import com.jaxon.smart_gym.profile.internal.entity.BodyMetric;
import com.jaxon.smart_gym.profile.internal.mapper.BodyMetricMapper;
import com.jaxon.smart_gym.workout.internal.entity.WorkoutPlan;
import com.jaxon.smart_gym.workout.internal.entity.WorkoutPlanDay;
import com.jaxon.smart_gym.workout.internal.entity.WorkoutSession;
import com.jaxon.smart_gym.workout.internal.mapper.WorkoutPlanDayMapper;
import com.jaxon.smart_gym.workout.internal.mapper.WorkoutPlanMapper;
import com.jaxon.smart_gym.workout.internal.mapper.WorkoutSessionMapper;

@Service
public class DashboardService {

    private static final String ACTIVE = "ACTIVE";
    private static final String COMPLETED = "COMPLETED";
    private static final String IN_PROGRESS = "IN_PROGRESS";
    private static final int MAX_TREND_DAYS = 366;
    private static final List<String> QUICK_QUESTIONS = List.of(
            "今天训练前应该吃什么？",
            "帮我调整今天的训练"
    );

    private final BodyMetricMapper bodyMetricMapper;
    private final DailyNutritionSummaryMapper dailyNutritionSummaryMapper;
    private final MealRecordMapper mealRecordMapper;
    private final NutritionTargetMapper nutritionTargetMapper;
    private final WorkoutPlanMapper workoutPlanMapper;
    private final WorkoutPlanDayMapper workoutPlanDayMapper;
    private final WorkoutSessionMapper workoutSessionMapper;
    private final Clock clock;

    public DashboardService(
            BodyMetricMapper bodyMetricMapper,
            DailyNutritionSummaryMapper dailyNutritionSummaryMapper,
            MealRecordMapper mealRecordMapper,
            NutritionTargetMapper nutritionTargetMapper,
            WorkoutPlanMapper workoutPlanMapper,
            WorkoutPlanDayMapper workoutPlanDayMapper,
            WorkoutSessionMapper workoutSessionMapper,
            Clock clock) {
        this.bodyMetricMapper = bodyMetricMapper;
        this.dailyNutritionSummaryMapper = dailyNutritionSummaryMapper;
        this.mealRecordMapper = mealRecordMapper;
        this.nutritionTargetMapper = nutritionTargetMapper;
        this.workoutPlanMapper = workoutPlanMapper;
        this.workoutPlanDayMapper = workoutPlanDayMapper;
        this.workoutSessionMapper = workoutSessionMapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DashboardOverviewResponse getOverview(Long userId, LocalDate requestedDate) {
        LocalDate date = requestedDate == null ? LocalDate.now(clock) : requestedDate;
        DashboardOverviewResponse.Nutrition nutrition = nutritionOverview(userId, date);
        BigDecimal latestWeight = latestWeight(userId, date);
        BigDecimal baselineWeight = baselineWeight(userId, date);
        BigDecimal weightChange = latestWeight == null || baselineWeight == null
                ? null
                : latestWeight.subtract(baselineWeight).setScale(2, RoundingMode.HALF_UP);

        return new DashboardOverviewResponse(
                date,
                todayWorkout(userId, date),
                nutrition,
                weeklyCompletionRate(userId, date),
                latestWeight,
                weightChange,
                QUICK_QUESTIONS
        );
    }

    @Transactional(readOnly = true)
    public DashboardTrendResponse getTrend(
            Long userId,
            LocalDate from,
            LocalDate to,
            String metric) {
        validateTrendRange(from, to);
        String normalizedMetric = normalizeMetric(metric);
        return switch (normalizedMetric) {
            case "WEIGHT" -> new DashboardTrendResponse(
                    normalizedMetric, "kg", weightTrend(userId, from, to));
            case "TRAINING_VOLUME" -> new DashboardTrendResponse(
                    normalizedMetric, "kg", trainingVolumeTrend(userId, from, to));
            case "NUTRITION" -> new DashboardTrendResponse(
                    normalizedMetric, "kcal", nutritionTrend(userId, from, to));
            default -> throw new IllegalStateException("Unexpected dashboard metric: " + normalizedMetric);
        };
    }

    private DashboardOverviewResponse.TodayWorkout todayWorkout(Long userId, LocalDate date) {
        WorkoutPlan plan = findPlanForDate(userId, date, loadActivePlans(userId, date, date));
        if (plan == null) {
            return null;
        }
        WorkoutPlanDay day = findDay(plan.getId(), date.getDayOfWeek().getValue());
        if (day == null) {
            return null;
        }

        List<WorkoutSession> sessions = workoutSessionMapper.selectList(
                new LambdaQueryWrapper<WorkoutSession>()
                        .eq(WorkoutSession::getUserId, userId)
                        .eq(WorkoutSession::getPlanDayId, day.getId())
                        .ge(WorkoutSession::getStartedAt, date.atStartOfDay())
                        .lt(WorkoutSession::getStartedAt, date.plusDays(1).atStartOfDay())
                        .orderByDesc(WorkoutSession::getStartedAt)
                        .orderByDesc(WorkoutSession::getId));

        String status = sessions.stream().anyMatch(s -> COMPLETED.equals(s.getStatus()))
                ? COMPLETED
                : sessions.stream().anyMatch(s -> IN_PROGRESS.equals(s.getStatus()))
                        ? IN_PROGRESS
                        : "NOT_STARTED";
        return new DashboardOverviewResponse.TodayWorkout(
                plan.getId(), day.getTitle(), day.getEstimatedMinutes(), status);
    }

    private BigDecimal weeklyCompletionRate(Long userId, LocalDate date) {
        LocalDate weekStart = date.minusDays(date.getDayOfWeek().getValue() - 1L);
        LocalDate weekEnd = weekStart.plusDays(6);
        List<WorkoutPlan> plans = loadActivePlans(userId, weekStart, weekEnd);
        Map<Long, List<WorkoutPlanDay>> daysByPlan = loadDays(plans);
        Map<LocalDate, Long> plannedDayIds = new LinkedHashMap<>();

        for (LocalDate current = weekStart; !current.isAfter(weekEnd); current = current.plusDays(1)) {
            LocalDate currentDate = current;
            WorkoutPlan plan = findPlanForDate(userId, currentDate, plans);
            if (plan == null) {
                continue;
            }
            daysByPlan.getOrDefault(plan.getId(), List.of()).stream()
                    .filter(day -> Integer.valueOf(currentDate.getDayOfWeek().getValue()).equals(day.getDayOfWeek()))
                    .findFirst()
                    .ifPresent(day -> plannedDayIds.put(currentDate, day.getId()));
        }

        if (plannedDayIds.isEmpty()) {
            return BigDecimal.ZERO.setScale(4);
        }

        List<WorkoutSession> sessions = workoutSessionMapper.selectList(
                new LambdaQueryWrapper<WorkoutSession>()
                        .eq(WorkoutSession::getUserId, userId)
                        .eq(WorkoutSession::getStatus, COMPLETED)
                        .ge(WorkoutSession::getStartedAt, weekStart.atStartOfDay())
                        .lt(WorkoutSession::getStartedAt, weekEnd.plusDays(1).atStartOfDay()));
        Set<Long> plannedIds = new HashSet<>(plannedDayIds.values());
        Set<LocalDate> completedDates = sessions.stream()
                .filter(session -> session.getPlanDayId() != null && plannedIds.contains(session.getPlanDayId()))
                .filter(session -> session.getStartedAt() != null)
                .map(session -> session.getStartedAt().toLocalDate())
                .filter(plannedDayIds::containsKey)
                .collect(Collectors.toSet());

        return BigDecimal.valueOf(completedDates.size())
                .divide(BigDecimal.valueOf(plannedDayIds.size()), 4, RoundingMode.HALF_UP);
    }

    private DashboardOverviewResponse.Nutrition nutritionOverview(Long userId, LocalDate date) {
        DailyNutritionSummary summary = dailyNutritionSummaryMapper.selectOne(
                new LambdaQueryWrapper<DailyNutritionSummary>()
                        .eq(DailyNutritionSummary::getUserId, userId)
                        .eq(DailyNutritionSummary::getSummaryDate, date));
        NutritionTarget target = latestTarget(userId, date);

        BigDecimal caloriesConsumed = summary == null
                ? mealRecords(userId, date).stream()
                        .map(MealRecord::getTotalCalories)
                        .filter(value -> value != null)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                : valueOrZero(summary.getConsumedCalories());
        BigDecimal proteinConsumed = summary == null
                ? mealRecords(userId, date).stream()
                        .map(MealRecord::getTotalProteinG)
                        .filter(value -> value != null)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                : valueOrZero(summary.getConsumedProteinG());

        BigDecimal caloriesTarget = summary != null && summary.getTargetCalories() != null
                ? summary.getTargetCalories()
                : target == null ? BigDecimal.ZERO : valueOrZero(target.getCalories());
        BigDecimal proteinTarget = summary != null && summary.getTargetProteinG() != null
                ? summary.getTargetProteinG()
                : target == null ? BigDecimal.ZERO : valueOrZero(target.getProteinG());

        return new DashboardOverviewResponse.Nutrition(
                caloriesConsumed, caloriesTarget, proteinConsumed, proteinTarget);
    }

    private BigDecimal latestWeight(Long userId, LocalDate date) {
        return bodyMetricMapper.selectList(
                        new LambdaQueryWrapper<BodyMetric>()
                                .eq(BodyMetric::getUserId, userId)
                                .isNotNull(BodyMetric::getWeightKg)
                                .lt(BodyMetric::getMeasuredAt, date.plusDays(1).atStartOfDay())
                                .orderByDesc(BodyMetric::getMeasuredAt)
                                .orderByDesc(BodyMetric::getId))
                .stream()
                .findFirst()
                .map(BodyMetric::getWeightKg)
                .orElse(null);
    }

    private BigDecimal baselineWeight(Long userId, LocalDate date) {
        LocalDateTime cutoff = date.minusDays(6).atStartOfDay();
        return bodyMetricMapper.selectList(
                        new LambdaQueryWrapper<BodyMetric>()
                                .eq(BodyMetric::getUserId, userId)
                                .isNotNull(BodyMetric::getWeightKg)
                                .lt(BodyMetric::getMeasuredAt, cutoff)
                                .orderByDesc(BodyMetric::getMeasuredAt)
                                .orderByDesc(BodyMetric::getId))
                .stream()
                .findFirst()
                .map(BodyMetric::getWeightKg)
                .orElse(null);
    }

    private List<DashboardTrendResponse.Point> weightTrend(Long userId, LocalDate from, LocalDate to) {
        Map<LocalDate, BodyMetric> latestByDate = new HashMap<>();
        bodyMetricMapper.selectList(
                        new LambdaQueryWrapper<BodyMetric>()
                                .eq(BodyMetric::getUserId, userId)
                                .isNotNull(BodyMetric::getWeightKg)
                                .ge(BodyMetric::getMeasuredAt, from.atStartOfDay())
                                .lt(BodyMetric::getMeasuredAt, to.plusDays(1).atStartOfDay())
                                .orderByAsc(BodyMetric::getMeasuredAt)
                                .orderByAsc(BodyMetric::getId))
                .forEach(metric -> latestByDate.put(metric.getMeasuredAt().toLocalDate(), metric));
        return latestByDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> point(entry.getKey(), entry.getValue().getWeightKg()))
                .toList();
    }

    private List<DashboardTrendResponse.Point> trainingVolumeTrend(Long userId, LocalDate from, LocalDate to) {
        Map<LocalDate, BigDecimal> volumeByDate = new HashMap<>();
        workoutSessionMapper.selectList(
                        new LambdaQueryWrapper<WorkoutSession>()
                                .eq(WorkoutSession::getUserId, userId)
                                .eq(WorkoutSession::getStatus, COMPLETED)
                                .ge(WorkoutSession::getStartedAt, from.atStartOfDay())
                                .lt(WorkoutSession::getStartedAt, to.plusDays(1).atStartOfDay())
                                .orderByAsc(WorkoutSession::getStartedAt))
                .forEach(session -> {
                    if (session.getStartedAt() != null) {
                        LocalDate day = session.getStartedAt().toLocalDate();
                        volumeByDate.merge(day, valueOrZero(session.getTotalVolumeKg()), BigDecimal::add);
                    }
                });
        return volumeByDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> point(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<DashboardTrendResponse.Point> nutritionTrend(Long userId, LocalDate from, LocalDate to) {
        Map<LocalDate, BigDecimal> caloriesByDate = new HashMap<>();
        Set<LocalDate> summaryDates = new HashSet<>();
        dailyNutritionSummaryMapper.selectList(
                        new LambdaQueryWrapper<DailyNutritionSummary>()
                                .eq(DailyNutritionSummary::getUserId, userId)
                                .ge(DailyNutritionSummary::getSummaryDate, from)
                                .le(DailyNutritionSummary::getSummaryDate, to)
                                .orderByAsc(DailyNutritionSummary::getSummaryDate))
                .forEach(summary -> {
                    summaryDates.add(summary.getSummaryDate());
                    caloriesByDate.put(summary.getSummaryDate(), valueOrZero(summary.getConsumedCalories()));
                });

        mealRecords(userId, from, to).forEach(record -> {
            if (record.getEatenAt() != null && !summaryDates.contains(record.getEatenAt().toLocalDate())) {
                caloriesByDate.merge(
                        record.getEatenAt().toLocalDate(),
                        valueOrZero(record.getTotalCalories()),
                        BigDecimal::add);
            }
        });

        return caloriesByDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> point(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<MealRecord> mealRecords(Long userId, LocalDate date) {
        return mealRecords(userId, date, date);
    }

    private List<MealRecord> mealRecords(Long userId, LocalDate from, LocalDate to) {
        return mealRecordMapper.selectList(
                new LambdaQueryWrapper<MealRecord>()
                        .eq(MealRecord::getUserId, userId)
                        .ge(MealRecord::getEatenAt, from.atStartOfDay())
                        .lt(MealRecord::getEatenAt, to.plusDays(1).atStartOfDay())
                        .orderByAsc(MealRecord::getEatenAt)
                        .orderByAsc(MealRecord::getId));
    }

    private NutritionTarget latestTarget(Long userId, LocalDate date) {
        return nutritionTargetMapper.selectList(
                        new LambdaQueryWrapper<NutritionTarget>()
                                .eq(NutritionTarget::getUserId, userId)
                                .le(NutritionTarget::getEffectiveDate, date)
                                .orderByDesc(NutritionTarget::getEffectiveDate)
                                .orderByDesc(NutritionTarget::getId))
                .stream()
                .findFirst()
                .orElse(null);
    }

    private List<WorkoutPlan> loadActivePlans(Long userId, LocalDate from, LocalDate to) {
        return workoutPlanMapper.selectList(
                new LambdaQueryWrapper<WorkoutPlan>()
                        .eq(WorkoutPlan::getUserId, userId)
                        .eq(WorkoutPlan::getStatus, ACTIVE)
                        .isNull(WorkoutPlan::getDeletedAt)
                        .and(wrapper -> wrapper.isNull(WorkoutPlan::getStartsOn)
                                .or().le(WorkoutPlan::getStartsOn, to))
                        .and(wrapper -> wrapper.isNull(WorkoutPlan::getEndsOn)
                                .or().ge(WorkoutPlan::getEndsOn, from))
                        .orderByDesc(WorkoutPlan::getStartsOn)
                        .orderByDesc(WorkoutPlan::getId));
    }

    private Map<Long, List<WorkoutPlanDay>> loadDays(List<WorkoutPlan> plans) {
        Map<Long, List<WorkoutPlanDay>> result = new HashMap<>();
        for (WorkoutPlan plan : plans) {
            result.put(plan.getId(), workoutPlanDayMapper.selectList(
                    new LambdaQueryWrapper<WorkoutPlanDay>()
                            .eq(WorkoutPlanDay::getPlanId, plan.getId())
                            .orderByAsc(WorkoutPlanDay::getDayNo)
                            .orderByAsc(WorkoutPlanDay::getId)));
        }
        return result;
    }

    private WorkoutPlan findPlanForDate(Long userId, LocalDate date, List<WorkoutPlan> plans) {
        return plans.stream()
                .filter(plan -> isEffective(plan, date))
                .findFirst()
                .orElse(null);
    }

    private WorkoutPlanDay findDay(Long planId, int dayOfWeek) {
        return workoutPlanDayMapper.selectList(
                        new LambdaQueryWrapper<WorkoutPlanDay>()
                                .eq(WorkoutPlanDay::getPlanId, planId)
                                .eq(WorkoutPlanDay::getDayOfWeek, dayOfWeek)
                                .orderByAsc(WorkoutPlanDay::getDayNo)
                                .orderByAsc(WorkoutPlanDay::getId))
                .stream()
                .findFirst()
                .orElse(null);
    }

    private boolean isEffective(WorkoutPlan plan, LocalDate date) {
        return (plan.getStartsOn() == null || !date.isBefore(plan.getStartsOn()))
                && (plan.getEndsOn() == null || !date.isAfter(plan.getEndsOn()));
    }

    private DashboardTrendResponse.Point point(LocalDate date, BigDecimal value) {
        return new DashboardTrendResponse.Point(date, valueOrZero(value).setScale(2, RoundingMode.HALF_UP));
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private void validateTrendRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new BusinessException("VALIDATION_ERROR", "趋势起止日期不能为空", HttpStatus.BAD_REQUEST);
        }
        if (from.isAfter(to)) {
            throw new BusinessException("VALIDATION_ERROR", "开始日期不能晚于结束日期", HttpStatus.BAD_REQUEST);
        }
        if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_TREND_DAYS) {
            throw new BusinessException("VALIDATION_ERROR", "趋势查询范围不能超过 366 天", HttpStatus.BAD_REQUEST);
        }
    }

    private String normalizeMetric(String metric) {
        if (metric == null || metric.isBlank()) {
            throw new BusinessException("VALIDATION_ERROR", "趋势指标不能为空", HttpStatus.BAD_REQUEST);
        }
        String normalized = metric.trim().toUpperCase();
        if (!Set.of("WEIGHT", "TRAINING_VOLUME", "NUTRITION").contains(normalized)) {
            throw new BusinessException("VALIDATION_ERROR", "趋势指标不支持: " + metric, HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }
}
