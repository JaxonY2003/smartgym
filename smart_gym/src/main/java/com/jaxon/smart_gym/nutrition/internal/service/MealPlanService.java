package com.jaxon.smart_gym.nutrition.internal.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jaxon.smart_gym.common.exception.BusinessException;
import com.jaxon.smart_gym.common.response.PageResponse;
import com.jaxon.smart_gym.nutrition.internal.dto.request.MealPlanCreateRequest;
import com.jaxon.smart_gym.nutrition.internal.dto.request.MealPlanReplaceItemRequest;
import com.jaxon.smart_gym.nutrition.internal.dto.request.MealPlanUpdateRequest;
import com.jaxon.smart_gym.nutrition.internal.dto.response.MealPlanResponse;
import com.jaxon.smart_gym.nutrition.internal.entity.Food;
import com.jaxon.smart_gym.nutrition.internal.entity.FoodAllergen;
import com.jaxon.smart_gym.nutrition.internal.entity.FoodNutrition;
import com.jaxon.smart_gym.nutrition.internal.entity.MealPlan;
import com.jaxon.smart_gym.nutrition.internal.entity.MealPlanDay;
import com.jaxon.smart_gym.nutrition.internal.entity.MealPlanItem;
import com.jaxon.smart_gym.nutrition.internal.entity.MealPlanMeal;
import com.jaxon.smart_gym.nutrition.internal.mapper.FoodAllergenMapper;
import com.jaxon.smart_gym.nutrition.internal.mapper.FoodMapper;
import com.jaxon.smart_gym.nutrition.internal.mapper.FoodNutritionMapper;
import com.jaxon.smart_gym.nutrition.internal.mapper.MealPlanDayMapper;
import com.jaxon.smart_gym.nutrition.internal.mapper.MealPlanItemMapper;
import com.jaxon.smart_gym.nutrition.internal.mapper.MealPlanMapper;
import com.jaxon.smart_gym.nutrition.internal.mapper.MealPlanMealMapper;
import com.jaxon.smart_gym.profile.internal.entity.FoodAllergy;
import com.jaxon.smart_gym.profile.internal.mapper.FoodAllergyMapper;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Service
public class MealPlanService {

    private static final String MANUAL = "MANUAL";
    private static final String DRAFT = "DRAFT";
    private static final String ACTIVE = "ACTIVE";
    private static final String ARCHIVED = "ARCHIVED";
    private static final int SCALE = 2;

    private final MealPlanMapper mealPlanMapper;
    private final MealPlanDayMapper mealPlanDayMapper;
    private final MealPlanMealMapper mealPlanMealMapper;
    private final MealPlanItemMapper mealPlanItemMapper;
    private final FoodMapper foodMapper;
    private final FoodNutritionMapper foodNutritionMapper;
    private final FoodAllergenMapper foodAllergenMapper;
    private final FoodAllergyMapper foodAllergyMapper;
    private final JsonMapper objectMapper;

    public MealPlanService(
            MealPlanMapper mealPlanMapper,
            MealPlanDayMapper mealPlanDayMapper,
            MealPlanMealMapper mealPlanMealMapper,
            MealPlanItemMapper mealPlanItemMapper,
            FoodMapper foodMapper,
            FoodNutritionMapper foodNutritionMapper,
            FoodAllergenMapper foodAllergenMapper,
            FoodAllergyMapper foodAllergyMapper,
            JsonMapper objectMapper) {
        this.mealPlanMapper = mealPlanMapper;
        this.mealPlanDayMapper = mealPlanDayMapper;
        this.mealPlanMealMapper = mealPlanMealMapper;
        this.mealPlanItemMapper = mealPlanItemMapper;
        this.foodMapper = foodMapper;
        this.foodNutritionMapper = foodNutritionMapper;
        this.foodAllergenMapper = foodAllergenMapper;
        this.foodAllergyMapper = foodAllergyMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<MealPlanResponse> listPlans(
            Long userId,
            String status,
            long page,
            long pageSize) {
        LambdaQueryWrapper<MealPlan> query = new LambdaQueryWrapper<MealPlan>()
                .eq(MealPlan::getUserId, userId)
                .isNull(MealPlan::getDeletedAt)
                .orderByDesc(MealPlan::getUpdatedAt)
                .orderByDesc(MealPlan::getId);
        if (status != null && !status.isBlank()) {
            query.eq(MealPlan::getStatus, normalizeStatus(status));
        }
        Page<MealPlan> result = mealPlanMapper.selectPage(new Page<>(page, pageSize), query);
        List<MealPlanResponse> items = result.getRecords().stream()
                .map(plan -> toResponse(plan, loadDays(plan.getId())))
                .toList();
        return PageResponse.of(items, result.getCurrent(), result.getSize(), result.getTotal());
    }

    @Transactional(readOnly = true)
    public MealPlanResponse getPlan(Long userId, Long planId) {
        MealPlan plan = requirePlan(userId, planId);
        return toResponse(plan, loadDays(plan.getId()));
    }

    @Transactional
    public MealPlanResponse createPlan(Long userId, MealPlanCreateRequest request) {
        Set<Long> allergyIds = activeAllergenIds(userId);
        List<PreparedMeal> preparedMeals = prepareMeals(request.meals(), allergyIds);
        NutritionTotals totals = sumMeals(preparedMeals);
        LocalDate date = request.date();

        MealPlan plan = new MealPlan();
        plan.setUserId(userId);
        plan.setName(request.name().trim());
        plan.setSource(MANUAL);
        plan.setGoal(normalizeNullableUpper(request.goal()));
        plan.setStatus(DRAFT);
        plan.setStartsOn(date);
        plan.setEndsOn(date);
        applyTotals(plan, totals);
        plan.setWarningsJson(writeJson(List.of()));
        plan.setAssumptionsJson(writeJson(request.assumptions() == null ? List.of() : request.assumptions()));
        plan.setVersion(1);
        mealPlanMapper.insert(plan);

        insertMeals(plan.getId(), date, preparedMeals);
        return getPlan(userId, plan.getId());
    }

    @Transactional
    public MealPlanResponse updatePlan(
            Long userId,
            Long planId,
            MealPlanUpdateRequest request) {
        MealPlan plan = requirePlan(userId, planId);
        LocalDate date = request.date() == null ? plan.getStartsOn() : request.date();
        if (date == null) {
            throw new BusinessException("VALIDATION_ERROR", "计划日期不能为空", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        List<PreparedMeal> preparedMeals = null;
        if (request.meals() != null) {
            preparedMeals = prepareMeals(request.meals(), activeAllergenIds(userId));
            replaceMeals(plan.getId(), date, preparedMeals);
            applyTotals(plan, sumMeals(preparedMeals));
        } else if (request.date() != null && !date.equals(plan.getStartsOn())) {
            updatePlanDate(plan.getId(), date);
        }

        if (request.name() != null) {
            if (request.name().isBlank()) {
                throw new BusinessException("VALIDATION_ERROR", "计划名称不能为空", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            plan.setName(request.name().trim());
        }
        if (request.goal() != null) {
            plan.setGoal(normalizeNullableUpper(request.goal()));
        }
        if (request.date() != null) {
            plan.setStartsOn(date);
            plan.setEndsOn(date);
        }
        if (request.assumptions() != null) {
            plan.setAssumptionsJson(writeJson(request.assumptions()));
        }
        plan.setVersion(nextVersion(plan.getVersion()));
        mealPlanMapper.updateById(plan);
        return getPlan(userId, planId);
    }

    @Transactional
    public MealPlanResponse activatePlan(Long userId, Long planId) {
        MealPlan plan = requirePlan(userId, planId);
        mealPlanMapper.update(
                null,
                new LambdaUpdateWrapper<MealPlan>()
                        .eq(MealPlan::getUserId, userId)
                        .eq(MealPlan::getStatus, ACTIVE)
                        .ne(MealPlan::getId, planId)
                        .isNull(MealPlan::getDeletedAt)
                        .set(MealPlan::getStatus, ARCHIVED));
        plan.setStatus(ACTIVE);
        plan.setVersion(nextVersion(plan.getVersion()));
        mealPlanMapper.updateById(plan);
        return getPlan(userId, planId);
    }

    @Transactional
    public MealPlanResponse replaceItem(
            Long userId,
            Long planId,
            MealPlanReplaceItemRequest request) {
        MealPlan plan = requirePlan(userId, planId);
        MealPlanItem item = mealPlanItemMapper.selectById(request.itemId());
        if (item == null) {
            throw new BusinessException("MEAL_PLAN_ITEM_NOT_FOUND", "饮食计划食物项不存在", HttpStatus.NOT_FOUND);
        }
        MealPlanMeal meal = mealPlanMealMapper.selectById(item.getMealPlanMealId());
        MealPlanDay day = meal == null ? null : mealPlanDayMapper.selectById(meal.getMealPlanDayId());
        if (meal == null || day == null || !planId.equals(day.getMealPlanId())) {
            throw new BusinessException("MEAL_PLAN_ITEM_NOT_FOUND", "饮食计划食物项不存在", HttpStatus.NOT_FOUND);
        }

        Set<Long> allergyIds = activeAllergenIds(userId);
        PreparedItem replacement = prepareItem(
                request.replacementFoodId(), item.getAmount(), item.getUnit(), request.reason(), allergyIds);
        item.setFoodId(replacement.food().getId());
        item.setRecipeId(null);
        item.setFoodNameSnapshot(replacement.food().getName());
        item.setEstimatedCalories(replacement.totals().calories());
        item.setEstimatedProteinG(replacement.totals().proteinG());
        item.setEstimatedCarbohydrateG(replacement.totals().carbohydrateG());
        item.setEstimatedFatG(replacement.totals().fatG());
        item.setEstimatedFiberG(replacement.totals().fiberG());
        item.setNote(normalizeNullable(request.reason()));
        mealPlanItemMapper.updateById(item);

        recalculatePlan(plan);
        plan.setVersion(nextVersion(plan.getVersion()));
        mealPlanMapper.updateById(plan);
        return getPlan(userId, planId);
    }

    private MealPlan requirePlan(Long userId, Long planId) {
        MealPlan plan = mealPlanMapper.selectOne(
                new LambdaQueryWrapper<MealPlan>()
                        .eq(MealPlan::getId, planId)
                        .eq(MealPlan::getUserId, userId)
                        .isNull(MealPlan::getDeletedAt));
        if (plan == null) {
            throw new BusinessException("MEAL_PLAN_NOT_FOUND", "饮食计划不存在", HttpStatus.NOT_FOUND);
        }
        return plan;
    }

    private List<PreparedMeal> prepareMeals(
            List<MealPlanCreateRequest.MealRequest> mealRequests,
            Set<Long> allergyIds) {
        List<PreparedMeal> preparedMeals = new ArrayList<>();
        for (MealPlanCreateRequest.MealRequest mealRequest : mealRequests) {
            List<PreparedItem> items = new ArrayList<>();
            int itemIndex = 0;
            for (MealPlanCreateRequest.ItemRequest itemRequest : mealRequest.items()) {
                items.add(prepareItem(
                        itemRequest.foodId(), itemRequest.amount(), itemRequest.unit(),
                        itemRequest.note(), allergyIds, itemIndex++));
            }
            preparedMeals.add(new PreparedMeal(
                    normalizeRequiredUpper(mealRequest.mealType(), "餐次不能为空"),
                    mealRequest.time(),
                    normalizeNullable(mealRequest.notes()),
                    items));
        }
        return preparedMeals;
    }

    private PreparedItem prepareItem(
            Long foodId,
            BigDecimal amount,
            String unit,
            String note,
            Set<Long> allergyIds) {
        return prepareItem(foodId, amount, unit, note, allergyIds, 0);
    }

    private PreparedItem prepareItem(
            Long foodId,
            BigDecimal amount,
            String unit,
            String note,
            Set<Long> allergyIds,
            int sortOrder) {
        Food food = foodMapper.selectOne(
                new LambdaQueryWrapper<Food>()
                        .eq(Food::getId, foodId)
                        .eq(Food::getStatus, "ACTIVE"));
        if (food == null) {
            throw new BusinessException("FOOD_NOT_FOUND", "食物不存在", HttpStatus.NOT_FOUND);
        }
        ensureNoAllergen(foodId, allergyIds);
        String normalizedUnit = normalizeUnit(unit);
        FoodNutrition nutrition = findNutrition(foodId, normalizedUnit);
        BigDecimal multiplier = amount.divide(nutrition.getServingAmount(), 8, RoundingMode.HALF_UP);
        NutritionTotals totals = new NutritionTotals(
                multiply(nutrition.getCalories(), multiplier),
                multiply(nutrition.getProteinG(), multiplier),
                multiply(nutrition.getCarbohydrateG(), multiplier),
                multiply(nutrition.getFatG(), multiplier),
                multiply(nutrition.getFiberG(), multiplier));
        return new PreparedItem(food, amount, normalizedUnit, totals, normalizeNullable(note), sortOrder);
    }

    private FoodNutrition findNutrition(Long foodId, String unit) {
        List<FoodNutrition> rows = foodNutritionMapper.selectList(
                new LambdaQueryWrapper<FoodNutrition>()
                        .eq(FoodNutrition::getFoodId, foodId)
                        .eq(FoodNutrition::getServingUnit, unit)
                        .orderByAsc(FoodNutrition::getId));
        if (rows.isEmpty()) {
            throw new BusinessException(
                    "FOOD_UNIT_NOT_SUPPORTED",
                    "该食物不支持单位: " + unit,
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return rows.stream()
                .min(Comparator
                        .comparing((FoodNutrition row) -> BigDecimal.valueOf(100).compareTo(row.getServingAmount()) != 0)
                        .thenComparing(FoodNutrition::getId))
                .orElseThrow();
    }

    private void ensureNoAllergen(Long foodId, Set<Long> allergyIds) {
        if (allergyIds.isEmpty()) {
            return;
        }
        List<FoodAllergen> links = foodAllergenMapper.selectByFoodIds(List.of(foodId));
        boolean conflict = links.stream().anyMatch(link -> allergyIds.contains(link.getAllergenId()));
        if (conflict) {
            throw new BusinessException(
                    "FOOD_ALLERGEN_CONFLICT",
                    "食物与当前用户的过敏原冲突",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private Set<Long> activeAllergenIds(Long userId) {
        return foodAllergyMapper.selectList(
                        new LambdaQueryWrapper<FoodAllergy>()
                                .eq(FoodAllergy::getUserId, userId)
                                .eq(FoodAllergy::getActive, true))
                .stream()
                .map(FoodAllergy::getAllergenId)
                .collect(java.util.stream.Collectors.toSet());
    }

    private void insertMeals(
            Long planId,
            LocalDate date,
            List<PreparedMeal> preparedMeals) {
        MealPlanDay day = new MealPlanDay();
        day.setMealPlanId(planId);
        day.setPlanDate(date);
        day.setDayIndex(1);
        mealPlanDayMapper.insert(day);
        for (int mealIndex = 0; mealIndex < preparedMeals.size(); mealIndex++) {
            PreparedMeal preparedMeal = preparedMeals.get(mealIndex);
            MealPlanMeal meal = new MealPlanMeal();
            meal.setMealPlanDayId(day.getId());
            meal.setMealType(preparedMeal.mealType());
            meal.setMealTime(preparedMeal.time());
            meal.setSortOrder(mealIndex);
            meal.setNotes(preparedMeal.notes());
            mealPlanMealMapper.insert(meal);
            insertItems(meal.getId(), preparedMeal.items());
        }
    }

    private void insertItems(Long mealId, List<PreparedItem> preparedItems) {
        for (PreparedItem preparedItem : preparedItems) {
            MealPlanItem item = new MealPlanItem();
            item.setMealPlanMealId(mealId);
            item.setFoodId(preparedItem.food().getId());
            item.setFoodNameSnapshot(preparedItem.food().getName());
            item.setAmount(preparedItem.amount());
            item.setUnit(preparedItem.unit());
            item.setEstimatedCalories(preparedItem.totals().calories());
            item.setEstimatedProteinG(preparedItem.totals().proteinG());
            item.setEstimatedCarbohydrateG(preparedItem.totals().carbohydrateG());
            item.setEstimatedFatG(preparedItem.totals().fatG());
            item.setEstimatedFiberG(preparedItem.totals().fiberG());
            item.setNote(preparedItem.note());
            item.setSortOrder(preparedItem.sortOrder());
            mealPlanItemMapper.insert(item);
        }
    }

    private void replaceMeals(Long planId, LocalDate date, List<PreparedMeal> preparedMeals) {
        List<MealPlanDay> days = mealPlanDayMapper.selectList(
                new LambdaQueryWrapper<MealPlanDay>().eq(MealPlanDay::getMealPlanId, planId));
        for (MealPlanDay day : days) {
            List<MealPlanMeal> meals = mealPlanMealMapper.selectList(
                    new LambdaQueryWrapper<MealPlanMeal>().eq(MealPlanMeal::getMealPlanDayId, day.getId()));
            for (MealPlanMeal meal : meals) {
                mealPlanItemMapper.delete(new LambdaQueryWrapper<MealPlanItem>()
                        .eq(MealPlanItem::getMealPlanMealId, meal.getId()));
            }
            for (MealPlanMeal meal : meals) {
                mealPlanMealMapper.deleteById(meal.getId());
            }
            mealPlanDayMapper.deleteById(day.getId());
        }
        insertMeals(planId, date, preparedMeals);
    }

    private void updatePlanDate(Long planId, LocalDate date) {
        mealPlanDayMapper.update(
                null,
                new LambdaUpdateWrapper<MealPlanDay>()
                        .eq(MealPlanDay::getMealPlanId, planId)
                        .set(MealPlanDay::getPlanDate, date));
    }

    private List<MealPlanDay> loadDays(Long planId) {
        List<MealPlanDay> days = mealPlanDayMapper.selectList(
                new LambdaQueryWrapper<MealPlanDay>()
                        .eq(MealPlanDay::getMealPlanId, planId)
                        .orderByAsc(MealPlanDay::getDayIndex)
                        .orderByAsc(MealPlanDay::getId));
        return days;
    }

    private MealPlanResponse toResponse(MealPlan plan, List<MealPlanDay> days) {
        List<MealPlanResponse.DayResponse> dayResponses = days.stream()
                .map(day -> new MealPlanResponse.DayResponse(
                        day.getId(), day.getPlanDate(), day.getDayIndex(), loadMealResponses(day.getId())))
                .toList();
        return new MealPlanResponse(
                plan.getId(), plan.getName(), plan.getStartsOn(), plan.getSource(), plan.getGoal(),
                plan.getStatus(), new MealPlanResponse.EstimatedNutrition(
                        plan.getEstimatedCalories(), plan.getEstimatedProteinG(),
                        plan.getEstimatedCarbohydrateG(), plan.getEstimatedFatG(), plan.getEstimatedFiberG()),
                readJsonList(plan.getWarningsJson()), readJsonList(plan.getAssumptionsJson()), plan.getVersion(),
                dayResponses, toUtc(plan.getCreatedAt()), toUtc(plan.getUpdatedAt()));
    }

    private List<MealPlanResponse.MealResponse> loadMealResponses(Long dayId) {
        return mealPlanMealMapper.selectList(
                        new LambdaQueryWrapper<MealPlanMeal>()
                                .eq(MealPlanMeal::getMealPlanDayId, dayId)
                                .orderByAsc(MealPlanMeal::getSortOrder)
                                .orderByAsc(MealPlanMeal::getId))
                .stream()
                .map(meal -> new MealPlanResponse.MealResponse(
                        meal.getId(), meal.getMealType(), meal.getMealTime(), meal.getNotes(),
                        mealPlanItemMapper.selectList(
                                        new LambdaQueryWrapper<MealPlanItem>()
                                                .eq(MealPlanItem::getMealPlanMealId, meal.getId())
                                                .orderByAsc(MealPlanItem::getSortOrder)
                                                .orderByAsc(MealPlanItem::getId))
                                .stream()
                                .map(item -> new MealPlanResponse.ItemResponse(
                                        item.getId(), item.getFoodId(), item.getFoodNameSnapshot(), item.getAmount(),
                                        item.getUnit(), new MealPlanResponse.EstimatedNutrition(
                                                item.getEstimatedCalories(), item.getEstimatedProteinG(),
                                                item.getEstimatedCarbohydrateG(), item.getEstimatedFatG(),
                                                item.getEstimatedFiberG()), item.getNote(), item.getSortOrder()))
                                .toList()))
                .toList();
    }

    private void recalculatePlan(MealPlan plan) {
        List<MealPlanDay> days = loadDays(plan.getId());
        NutritionTotals totals = NutritionTotals.empty();
        for (MealPlanDay day : days) {
            List<MealPlanMeal> meals = mealPlanMealMapper.selectList(
                    new LambdaQueryWrapper<MealPlanMeal>().eq(MealPlanMeal::getMealPlanDayId, day.getId()));
            for (MealPlanMeal meal : meals) {
                List<MealPlanItem> items = mealPlanItemMapper.selectList(
                        new LambdaQueryWrapper<MealPlanItem>().eq(MealPlanItem::getMealPlanMealId, meal.getId()));
                for (MealPlanItem item : items) {
                    totals = totals.add(new NutritionTotals(
                            item.getEstimatedCalories(), item.getEstimatedProteinG(),
                            item.getEstimatedCarbohydrateG(), item.getEstimatedFatG(), item.getEstimatedFiberG()));
                }
            }
        }
        applyTotals(plan, totals);
    }

    private NutritionTotals sumMeals(List<PreparedMeal> meals) {
        NutritionTotals totals = NutritionTotals.empty();
        for (PreparedMeal meal : meals) {
            for (PreparedItem item : meal.items()) {
                totals = totals.add(item.totals());
            }
        }
        return totals;
    }

    private void applyTotals(MealPlan plan, NutritionTotals totals) {
        plan.setEstimatedCalories(totals.calories());
        plan.setEstimatedProteinG(totals.proteinG());
        plan.setEstimatedCarbohydrateG(totals.carbohydrateG());
        plan.setEstimatedFatG(totals.fatG());
        plan.setEstimatedFiberG(totals.fiberG());
    }

    private String normalizeStatus(String value) {
        String status = value.trim().toUpperCase(Locale.ROOT);
        if (!DRAFT.equals(status) && !ACTIVE.equals(status) && !ARCHIVED.equals(status)) {
            throw new BusinessException("VALIDATION_ERROR", "计划状态不支持: " + status, HttpStatus.BAD_REQUEST);
        }
        return status;
    }

    private String normalizeRequiredUpper(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException("VALIDATION_ERROR", message, HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeNullableUpper(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeUnit(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException("VALIDATION_ERROR", "食物单位不能为空", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Integer nextVersion(Integer version) {
        return version == null ? 1 : version + 1;
    }

    private String writeJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (RuntimeException exception) {
            throw new BusinessException("INTERNAL_ERROR", "计划数据序列化失败", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private List<String> readJsonList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() { });
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    private OffsetDateTime toUtc(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }

    private BigDecimal multiply(BigDecimal value, BigDecimal multiplier) {
        return value.multiply(multiplier).setScale(SCALE, RoundingMode.HALF_UP);
    }

    private record PreparedMeal(
            String mealType,
            LocalTime time,
            String notes,
            List<PreparedItem> items) {
    }

    private record PreparedItem(
            Food food,
            BigDecimal amount,
            String unit,
            NutritionTotals totals,
            String note,
            int sortOrder) {
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
                    calories.add(other.calories()), proteinG.add(other.proteinG()),
                    carbohydrateG.add(other.carbohydrateG()), fatG.add(other.fatG()),
                    fiberG.add(other.fiberG()));
        }
    }
}
