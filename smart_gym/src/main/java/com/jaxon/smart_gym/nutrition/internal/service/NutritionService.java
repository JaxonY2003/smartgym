package com.jaxon.smart_gym.nutrition.internal.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jaxon.smart_gym.common.exception.BusinessException;
import com.jaxon.smart_gym.common.response.PageResponse;
import com.jaxon.smart_gym.nutrition.internal.dto.request.MealRecordCreateRequest;
import com.jaxon.smart_gym.nutrition.internal.dto.request.NutritionTargetRequest;
import com.jaxon.smart_gym.nutrition.internal.dto.request.WaterRecordCreateRequest;
import com.jaxon.smart_gym.nutrition.internal.dto.response.DailyNutritionSummaryResponse;
import com.jaxon.smart_gym.nutrition.internal.dto.response.MealRecordResponse;
import com.jaxon.smart_gym.nutrition.internal.dto.response.NutritionTargetResponse;
import com.jaxon.smart_gym.nutrition.internal.dto.response.NutritionTrendResponse;
import com.jaxon.smart_gym.nutrition.internal.dto.response.WaterRecordResponse;
import com.jaxon.smart_gym.nutrition.internal.entity.DailyNutritionSummary;
import com.jaxon.smart_gym.nutrition.internal.entity.Food;
import com.jaxon.smart_gym.nutrition.internal.entity.FoodNutrition;
import com.jaxon.smart_gym.nutrition.internal.entity.MealRecord;
import com.jaxon.smart_gym.nutrition.internal.entity.MealRecordItem;
import com.jaxon.smart_gym.nutrition.internal.entity.NutritionTarget;
import com.jaxon.smart_gym.nutrition.internal.entity.WaterRecord;
import com.jaxon.smart_gym.nutrition.internal.mapper.DailyNutritionSummaryMapper;
import com.jaxon.smart_gym.nutrition.internal.mapper.FoodMapper;
import com.jaxon.smart_gym.nutrition.internal.mapper.FoodNutritionMapper;
import com.jaxon.smart_gym.nutrition.internal.mapper.MealRecordItemMapper;
import com.jaxon.smart_gym.nutrition.internal.mapper.MealRecordMapper;
import com.jaxon.smart_gym.nutrition.internal.mapper.NutritionTargetMapper;
import com.jaxon.smart_gym.nutrition.internal.mapper.WaterRecordMapper;

@Service
public class NutritionService {

    private static final String MANUAL = "MANUAL";
    private static final String USER_CONFIRMED = "USER_CONFIRMED";
    private static final int DEFAULT_MEAL_COUNT = 3;
    private static final int SCALE = 2;

    private final NutritionTargetMapper nutritionTargetMapper;
    private final MealRecordMapper mealRecordMapper;
    private final MealRecordItemMapper mealRecordItemMapper;
    private final DailyNutritionSummaryMapper dailyNutritionSummaryMapper;
    private final WaterRecordMapper waterRecordMapper;
    private final FoodMapper foodMapper;
    private final FoodNutritionMapper foodNutritionMapper;
    private final Clock clock;

    public NutritionService(
            NutritionTargetMapper nutritionTargetMapper,
            MealRecordMapper mealRecordMapper,
            MealRecordItemMapper mealRecordItemMapper,
            DailyNutritionSummaryMapper dailyNutritionSummaryMapper,
            WaterRecordMapper waterRecordMapper,
            FoodMapper foodMapper,
            FoodNutritionMapper foodNutritionMapper,
            Clock clock) {
        this.nutritionTargetMapper = nutritionTargetMapper;
        this.mealRecordMapper = mealRecordMapper;
        this.mealRecordItemMapper = mealRecordItemMapper;
        this.dailyNutritionSummaryMapper = dailyNutritionSummaryMapper;
        this.waterRecordMapper = waterRecordMapper;
        this.foodMapper = foodMapper;
        this.foodNutritionMapper = foodNutritionMapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public NutritionTargetResponse getCurrentTarget(Long userId) {
        return toTargetResponse(findTarget(userId, LocalDate.now(clock)));
    }

    @Transactional
    public NutritionTargetResponse updateCurrentTarget(Long userId, NutritionTargetRequest request) {
        LocalDate effectiveDate = LocalDate.now(clock);
        NutritionTarget target = findTargetByExactDate(userId, effectiveDate);
        if (target == null) {
            target = new NutritionTarget();
            target.setUserId(userId);
            target.setEffectiveDate(effectiveDate);
        }
        target.setCalories(request.calories());
        target.setProteinG(request.proteinG());
        target.setCarbohydrateG(request.carbohydrateG());
        target.setFatG(request.fatG());
        target.setFiberG(request.fiberG());
        target.setSource(normalizeSource(request.source(), USER_CONFIRMED));
        if (target.getId() == null) {
            nutritionTargetMapper.insert(target);
        } else {
            nutritionTargetMapper.updateById(target);
        }
        refreshSummary(userId, effectiveDate);
        return toTargetResponse(findTargetByExactDate(userId, effectiveDate));
    }

    @Transactional
    public MealRecordResponse createMealRecord(Long userId, MealRecordCreateRequest request) {
        String mealType = normalizeRequired(request.mealType(), "餐次不能为空");
        String inputSource = normalizeSource(request.inputSource(), MANUAL);
        LocalDateTime eatenAt = toUtc(request.eatenAt());

        List<MealRecordItem> items = new ArrayList<>();
        NutritionTotals totals = NutritionTotals.empty();
        for (MealRecordCreateRequest.Item itemRequest : request.items()) {
            Food food = requireActiveFood(itemRequest.foodId());
            FoodNutrition nutrition = findNutrition(food.getId(), itemRequest.unit());
            BigDecimal multiplier = itemRequest.amount().divide(
                    nutrition.getServingAmount(), 8, RoundingMode.HALF_UP);
            NutritionTotals itemTotals = new NutritionTotals(
                    multiply(nutrition.getCalories(), multiplier),
                    multiply(nutrition.getProteinG(), multiplier),
                    multiply(nutrition.getCarbohydrateG(), multiplier),
                    multiply(nutrition.getFatG(), multiplier),
                    multiply(nutrition.getFiberG(), multiplier));
            totals = totals.add(itemTotals);

            MealRecordItem item = new MealRecordItem();
            item.setFoodId(food.getId());
            item.setFoodNameSnapshot(food.getName());
            item.setAmount(itemRequest.amount());
            item.setUnit(itemRequest.unit().trim());
            item.setEstimatedCalories(itemTotals.calories());
            item.setEstimatedProteinG(itemTotals.proteinG());
            item.setEstimatedCarbohydrateG(itemTotals.carbohydrateG());
            item.setEstimatedFatG(itemTotals.fatG());
            item.setEstimatedFiberG(itemTotals.fiberG());
            item.setInputSource(inputSource);
            item.setConfidence(BigDecimal.ONE.setScale(3));
            item.setConfirmedByUser(itemRequest.confirmedByUser() == null
                    || itemRequest.confirmedByUser());
            items.add(item);
        }

        MealRecord record = new MealRecord();
        record.setUserId(userId);
        record.setEatenAt(eatenAt);
        record.setMealType(mealType);
        record.setInputSource(inputSource);
        record.setTotalCalories(totals.calories());
        record.setTotalProteinG(totals.proteinG());
        record.setTotalCarbohydrateG(totals.carbohydrateG());
        record.setTotalFatG(totals.fatG());
        record.setTotalFiberG(totals.fiberG());
        record.setNote(normalizeNullable(request.note()));
        record.setConfirmedByUser(items.stream().allMatch(item -> Boolean.TRUE.equals(item.getConfirmedByUser())));
        mealRecordMapper.insert(record);

        for (MealRecordItem item : items) {
            item.setMealRecordId(record.getId());
            mealRecordItemMapper.insert(item);
        }

        refreshSummary(userId, eatenAt.toLocalDate());
        MealRecord persisted = mealRecordMapper.selectById(record.getId());
        return toMealRecordResponse(persisted, loadItems(record.getId()));
    }

    @Transactional(readOnly = true)
    public PageResponse<MealRecordResponse> listMealRecords(
            Long userId,
            LocalDate date,
            String mealType,
            long page,
            long pageSize) {
        LambdaQueryWrapper<MealRecord> query = new LambdaQueryWrapper<MealRecord>()
                .eq(MealRecord::getUserId, userId)
                .orderByDesc(MealRecord::getEatenAt)
                .orderByDesc(MealRecord::getId);
        if (date != null) {
            query.ge(MealRecord::getEatenAt, date.atStartOfDay())
                    .lt(MealRecord::getEatenAt, date.plusDays(1).atStartOfDay());
        }
        if (mealType != null && !mealType.isBlank()) {
            query.eq(MealRecord::getMealType, mealType.trim().toUpperCase(Locale.ROOT));
        }

        Page<MealRecord> result = mealRecordMapper.selectPage(
                new Page<>(page, pageSize), query);
        List<MealRecordResponse> items = result.getRecords().stream()
                .map(record -> toMealRecordResponse(record, loadItems(record.getId())))
                .toList();
        return PageResponse.of(items, result.getCurrent(), result.getSize(), result.getTotal());
    }

    @Transactional(readOnly = true)
    public DailyNutritionSummaryResponse getDailySummary(Long userId, LocalDate date) {
        LocalDate summaryDate = date == null ? LocalDate.now(clock) : date;
        DailyNutritionSummary summary = dailyNutritionSummaryMapper.selectOne(
                new LambdaQueryWrapper<DailyNutritionSummary>()
                        .eq(DailyNutritionSummary::getUserId, userId)
                        .eq(DailyNutritionSummary::getSummaryDate, summaryDate));
        if (summary == null) {
            summary = calculateSummary(userId, summaryDate);
        }
        return toSummaryResponse(summary);
    }

    @Transactional(readOnly = true)
    public NutritionTrendResponse getNutritionTrend(
            Long userId,
            LocalDate from,
            LocalDate to,
            String metric) {
        if (from == null || to == null) {
            throw new BusinessException("VALIDATION_ERROR", "趋势起止日期不能为空", HttpStatus.BAD_REQUEST);
        }
        if (from.isAfter(to)) {
            throw new BusinessException("VALIDATION_ERROR", "开始日期不能晚于结束日期", HttpStatus.BAD_REQUEST);
        }
        String normalizedMetric = normalizeMetric(metric);
        List<DailyNutritionSummary> summaries = dailyNutritionSummaryMapper.selectList(
                new LambdaQueryWrapper<DailyNutritionSummary>()
                        .eq(DailyNutritionSummary::getUserId, userId)
                        .ge(DailyNutritionSummary::getSummaryDate, from)
                        .le(DailyNutritionSummary::getSummaryDate, to)
                        .orderByAsc(DailyNutritionSummary::getSummaryDate));
        List<NutritionTrendResponse.Point> points = summaries.stream()
                .map(summary -> toTrendPoint(summary, normalizedMetric))
                .toList();
        return new NutritionTrendResponse(from, to, normalizedMetric, points);
    }

    @Transactional
    public WaterRecordResponse createWaterRecord(Long userId, WaterRecordCreateRequest request) {
        WaterRecord record = new WaterRecord();
        record.setUserId(userId);
        record.setRecordedAt(toUtc(request.recordedAt()));
        record.setAmountMl(request.amountMl());
        record.setSource(MANUAL);
        waterRecordMapper.insert(record);
        return toWaterResponse(waterRecordMapper.selectById(record.getId()));
    }

    @Transactional(readOnly = true)
    public List<WaterRecordResponse> listWaterRecords(Long userId, LocalDate date) {
        if (date == null) {
            throw new BusinessException("VALIDATION_ERROR", "日期不能为空", HttpStatus.BAD_REQUEST);
        }
        List<WaterRecord> records = waterRecordMapper.selectList(
                new LambdaQueryWrapper<WaterRecord>()
                        .eq(WaterRecord::getUserId, userId)
                        .ge(WaterRecord::getRecordedAt, date.atStartOfDay())
                        .lt(WaterRecord::getRecordedAt, date.plusDays(1).atStartOfDay())
                        .orderByAsc(WaterRecord::getRecordedAt)
                        .orderByAsc(WaterRecord::getId));
        return records.stream().map(this::toWaterResponse).toList();
    }

    private NutritionTarget findTarget(Long userId, LocalDate date) {
        return nutritionTargetMapper.selectOne(
                new LambdaQueryWrapper<NutritionTarget>()
                        .eq(NutritionTarget::getUserId, userId)
                        .le(NutritionTarget::getEffectiveDate, date)
                        .orderByDesc(NutritionTarget::getEffectiveDate)
                        .orderByDesc(NutritionTarget::getId)
                        .last("LIMIT 1"));
    }

    private NutritionTarget findTargetByExactDate(Long userId, LocalDate date) {
        return nutritionTargetMapper.selectOne(
                new LambdaQueryWrapper<NutritionTarget>()
                        .eq(NutritionTarget::getUserId, userId)
                        .eq(NutritionTarget::getEffectiveDate, date));
    }

    private Food requireActiveFood(Long foodId) {
        Food food = foodMapper.selectOne(
                new LambdaQueryWrapper<Food>()
                        .eq(Food::getId, foodId)
                        .eq(Food::getStatus, "ACTIVE"));
        if (food == null) {
            throw new BusinessException("FOOD_NOT_FOUND", "食物不存在", HttpStatus.NOT_FOUND);
        }
        return food;
    }

    private FoodNutrition findNutrition(Long foodId, String unit) {
        String normalizedUnit = normalizeUnit(unit);
        List<FoodNutrition> rows = foodNutritionMapper.selectList(
                new LambdaQueryWrapper<FoodNutrition>()
                        .eq(FoodNutrition::getFoodId, foodId)
                        .eq(FoodNutrition::getServingUnit, normalizedUnit)
                        .orderByAsc(FoodNutrition::getId));
        if (rows.isEmpty()) {
            throw new BusinessException(
                    "FOOD_UNIT_NOT_SUPPORTED",
                    "该食物不支持单位: " + normalizedUnit,
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return rows.stream()
                .min(Comparator
                        .comparing((FoodNutrition row) -> (BigDecimal.valueOf(100).compareTo(row.getServingAmount()) != 0))
                        .thenComparing(FoodNutrition::getId))
                .orElseThrow();
    }

    private List<MealRecordItem> loadItems(Long recordId) {
        return mealRecordItemMapper.selectList(
                new LambdaQueryWrapper<MealRecordItem>()
                        .eq(MealRecordItem::getMealRecordId, recordId)
                        .orderByAsc(MealRecordItem::getId));
    }

    private void refreshSummary(Long userId, LocalDate date) {
        DailyNutritionSummary summary = calculateSummary(userId, date);
        DailyNutritionSummary existing = dailyNutritionSummaryMapper.selectOne(
                new LambdaQueryWrapper<DailyNutritionSummary>()
                        .eq(DailyNutritionSummary::getUserId, userId)
                        .eq(DailyNutritionSummary::getSummaryDate, date));
        if (existing == null) {
            dailyNutritionSummaryMapper.insert(summary);
        } else {
            summary.setId(existing.getId());
            dailyNutritionSummaryMapper.updateById(summary);
        }
    }

    private DailyNutritionSummary calculateSummary(Long userId, LocalDate date) {
        List<MealRecord> records = mealRecordMapper.selectList(
                new LambdaQueryWrapper<MealRecord>()
                        .eq(MealRecord::getUserId, userId)
                        .ge(MealRecord::getEatenAt, date.atStartOfDay())
                        .lt(MealRecord::getEatenAt, date.plusDays(1).atStartOfDay()));
        NutritionTotals consumed = NutritionTotals.empty();
        for (MealRecord record : records) {
            consumed = consumed.add(new NutritionTotals(
                    record.getTotalCalories(),
                    record.getTotalProteinG(),
                    record.getTotalCarbohydrateG(),
                    record.getTotalFatG(),
                    record.getTotalFiberG()));
        }
        NutritionTarget target = findTarget(userId, date);
        DailyNutritionSummary summary = new DailyNutritionSummary();
        summary.setUserId(userId);
        summary.setSummaryDate(date);
        summary.setTargetCalories(target == null ? null : target.getCalories());
        summary.setTargetProteinG(target == null ? null : target.getProteinG());
        summary.setTargetCarbohydrateG(target == null ? null : target.getCarbohydrateG());
        summary.setTargetFatG(target == null ? null : target.getFatG());
        summary.setTargetFiberG(target == null ? null : target.getFiberG());
        summary.setConsumedCalories(consumed.calories());
        summary.setConsumedProteinG(consumed.proteinG());
        summary.setConsumedCarbohydrateG(consumed.carbohydrateG());
        summary.setConsumedFatG(consumed.fatG());
        summary.setConsumedFiberG(consumed.fiberG());
        summary.setMealCount(records.size());
        summary.setRecordCompleteness(BigDecimal.valueOf(Math.min(1d,
                records.size() / (double) DEFAULT_MEAL_COUNT)).setScale(4, RoundingMode.HALF_UP));
        summary.setCalculatedAt(LocalDateTime.now(clock));
        return summary;
    }

    private NutritionTrendResponse.Point toTrendPoint(
            DailyNutritionSummary summary,
            String metric) {
        MetricValue metricValue = metricValue(summary, metric);
        BigDecimal completionRate = ratio(metricValue.value(), metricValue.target());
        return new NutritionTrendResponse.Point(
                summary.getSummaryDate(), metricValue.value(), metricValue.target(), completionRate);
    }

    private MetricValue metricValue(DailyNutritionSummary summary, String metric) {
        return switch (metric) {
            case "CALORIES" -> new MetricValue(summary.getConsumedCalories(), summary.getTargetCalories());
            case "PROTEIN" -> new MetricValue(summary.getConsumedProteinG(), summary.getTargetProteinG());
            case "CARBOHYDRATE" -> new MetricValue(summary.getConsumedCarbohydrateG(), summary.getTargetCarbohydrateG());
            case "FAT" -> new MetricValue(summary.getConsumedFatG(), summary.getTargetFatG());
            case "FIBER" -> new MetricValue(summary.getConsumedFiberG(), summary.getTargetFiberG());
            default -> throw new BusinessException("VALIDATION_ERROR", "营养趋势指标不支持: " + metric, HttpStatus.BAD_REQUEST);
        };
    }

    private String normalizeMetric(String metric) {
        if (metric == null || metric.isBlank()) {
            return "CALORIES";
        }
        return metric.trim().toUpperCase(Locale.ROOT);
    }

    private DailyNutritionSummaryResponse toSummaryResponse(DailyNutritionSummary summary) {
        BigDecimal caloriesRate = ratio(summary.getConsumedCalories(), summary.getTargetCalories());
        BigDecimal proteinRate = ratio(summary.getConsumedProteinG(), summary.getTargetProteinG());
        BigDecimal carbohydrateRate = ratio(summary.getConsumedCarbohydrateG(), summary.getTargetCarbohydrateG());
        BigDecimal fatRate = ratio(summary.getConsumedFatG(), summary.getTargetFatG());
        return new DailyNutritionSummaryResponse(
                summary.getSummaryDate(),
                new DailyNutritionSummaryResponse.Target(
                        summary.getTargetCalories(), summary.getTargetProteinG(),
                        summary.getTargetCarbohydrateG(), summary.getTargetFatG(), summary.getTargetFiberG()),
                new DailyNutritionSummaryResponse.Consumed(
                        summary.getConsumedCalories(), summary.getConsumedProteinG(),
                        summary.getConsumedCarbohydrateG(), summary.getConsumedFatG(), summary.getConsumedFiberG()),
                new DailyNutritionSummaryResponse.CompletionRate(
                        caloriesRate, proteinRate, carbohydrateRate, fatRate),
                summary.getMealCount(),
                summary.getRecordCompleteness());
    }

    private NutritionTargetResponse toTargetResponse(NutritionTarget target) {
        if (target == null) {
            return null;
        }
        return new NutritionTargetResponse(
                target.getId(), target.getEffectiveDate(), target.getGoal(), target.getCalories(),
                target.getProteinG(), target.getCarbohydrateG(), target.getFatG(), target.getFiberG(),
                target.getSource(), toUtc(target.getCreatedAt()), toUtc(target.getUpdatedAt()));
    }

    private MealRecordResponse toMealRecordResponse(MealRecord record, List<MealRecordItem> items) {
        List<MealRecordResponse.ItemResponse> itemResponses = items.stream()
                .map(item -> new MealRecordResponse.ItemResponse(
                        item.getFoodId(), item.getFoodNameSnapshot(), item.getAmount(), item.getUnit(),
                        new MealRecordResponse.NutritionResponse(
                                item.getEstimatedCalories(), item.getEstimatedProteinG(),
                                item.getEstimatedCarbohydrateG(), item.getEstimatedFatG(), item.getEstimatedFiberG()),
                        item.getConfidence(), item.getConfirmedByUser()))
                .toList();
        return new MealRecordResponse(
                record.getId(), toUtc(record.getEatenAt()), record.getMealType(), itemResponses,
                new MealRecordResponse.NutritionResponse(
                        record.getTotalCalories(), record.getTotalProteinG(),
                        record.getTotalCarbohydrateG(), record.getTotalFatG(), record.getTotalFiberG()),
                record.getInputSource(), record.getNote(), record.getConfirmedByUser(),
                toUtc(record.getCreatedAt()), toUtc(record.getUpdatedAt()));
    }

    private WaterRecordResponse toWaterResponse(WaterRecord record) {
        return new WaterRecordResponse(
                record.getId(), toUtc(record.getRecordedAt()), record.getAmountMl(), record.getSource(),
                toUtc(record.getCreatedAt()), toUtc(record.getUpdatedAt()));
    }

    private BigDecimal ratio(BigDecimal value, BigDecimal target) {
        if (value == null || target == null || target.signum() <= 0) {
            return null;
        }
        return value.divide(target, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal multiply(BigDecimal value, BigDecimal multiplier) {
        return value.multiply(multiplier).setScale(SCALE, RoundingMode.HALF_UP);
    }

    private String normalizeSource(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeUnit(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException("VALIDATION_ERROR", "食物单位不能为空", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException("VALIDATION_ERROR", message, HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private LocalDateTime toUtc(OffsetDateTime value) {
        return value.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    private OffsetDateTime toUtc(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }

    private record MetricValue(BigDecimal value, BigDecimal target) {
    }

    private record NutritionTotals(
            BigDecimal calories,
            BigDecimal proteinG,
            BigDecimal carbohydrateG,
            BigDecimal fatG,
            BigDecimal fiberG) {

        private static NutritionTotals empty() {
            return new NutritionTotals(
                    BigDecimal.ZERO.setScale(SCALE), BigDecimal.ZERO.setScale(SCALE),
                    BigDecimal.ZERO.setScale(SCALE), BigDecimal.ZERO.setScale(SCALE),
                    BigDecimal.ZERO.setScale(SCALE));
        }

        private NutritionTotals add(NutritionTotals other) {
            return new NutritionTotals(
                    calories.add(Objects.requireNonNullElse(other.calories(), BigDecimal.ZERO)),
                    proteinG.add(Objects.requireNonNullElse(other.proteinG(), BigDecimal.ZERO)),
                    carbohydrateG.add(Objects.requireNonNullElse(other.carbohydrateG(), BigDecimal.ZERO)),
                    fatG.add(Objects.requireNonNullElse(other.fatG(), BigDecimal.ZERO)),
                    fiberG.add(Objects.requireNonNullElse(other.fiberG(), BigDecimal.ZERO)));
        }
    }
}
