package com.jaxon.smart_gym.nutrition.internal.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jaxon.smart_gym.common.exception.BusinessException;
import com.jaxon.smart_gym.common.response.PageResponse;
import com.jaxon.smart_gym.nutrition.internal.dto.response.FoodNutritionResponse;
import com.jaxon.smart_gym.nutrition.internal.dto.response.FoodResponse;
import com.jaxon.smart_gym.nutrition.internal.entity.Food;
import com.jaxon.smart_gym.nutrition.internal.entity.FoodAllergen;
import com.jaxon.smart_gym.nutrition.internal.entity.FoodCategory;
import com.jaxon.smart_gym.nutrition.internal.entity.FoodNutrition;
import com.jaxon.smart_gym.nutrition.internal.entity.FoodServing;
import com.jaxon.smart_gym.nutrition.internal.mapper.FoodAllergenMapper;
import com.jaxon.smart_gym.nutrition.internal.mapper.FoodCategoryMapper;
import com.jaxon.smart_gym.nutrition.internal.mapper.FoodMapper;
import com.jaxon.smart_gym.nutrition.internal.mapper.FoodNutritionMapper;
import com.jaxon.smart_gym.nutrition.internal.mapper.FoodServingMapper;
import com.jaxon.smart_gym.profile.internal.entity.Allergen;
import com.jaxon.smart_gym.profile.internal.mapper.AllergenMapper;

@Service
public class FoodCatalogService {

    private static final String ACTIVE = "ACTIVE";
    private static final BigDecimal CANONICAL_AMOUNT = BigDecimal.valueOf(100);
    private static final String CANONICAL_UNIT = "g";

    private final FoodMapper foodMapper;
    private final FoodCategoryMapper foodCategoryMapper;
    private final FoodNutritionMapper foodNutritionMapper;
    private final FoodServingMapper foodServingMapper;
    private final FoodAllergenMapper foodAllergenMapper;
    private final AllergenMapper allergenMapper;

    public FoodCatalogService(
            FoodMapper foodMapper,
            FoodCategoryMapper foodCategoryMapper,
            FoodNutritionMapper foodNutritionMapper,
            FoodServingMapper foodServingMapper,
            FoodAllergenMapper foodAllergenMapper,
            AllergenMapper allergenMapper) {
        this.foodMapper = foodMapper;
        this.foodCategoryMapper = foodCategoryMapper;
        this.foodNutritionMapper = foodNutritionMapper;
        this.foodServingMapper = foodServingMapper;
        this.foodAllergenMapper = foodAllergenMapper;
        this.allergenMapper = allergenMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<FoodResponse> listFoods(
            String keyword,
            String category,
            String excludeAllergens,
            long page,
            long pageSize) {
        LambdaQueryWrapper<Food> query = new LambdaQueryWrapper<Food>()
                .eq(Food::getStatus, ACTIVE)
                .orderByAsc(Food::getName)
                .orderByAsc(Food::getId);

        if (keyword != null && !keyword.isBlank()) {
            String normalizedKeyword = keyword.trim();
            query.and(wrapper -> wrapper
                    .like(Food::getName, normalizedKeyword)
                    .or()
                    .like(Food::getEnglishName, normalizedKeyword));
        }

        if (category != null && !category.isBlank()) {
            FoodCategory foodCategory = foodCategoryMapper.selectOne(
                    new LambdaQueryWrapper<FoodCategory>()
                            .eq(FoodCategory::getCode, category.trim().toUpperCase(Locale.ROOT))
                            .eq(FoodCategory::getStatus, ACTIVE));
            if (foodCategory == null) {
                return PageResponse.of(List.of(), page, pageSize, 0);
            }
            query.eq(Food::getCategoryId, foodCategory.getId());
        }

        applyAllergenExclusions(query, excludeAllergens);

        Page<Food> result = foodMapper.selectPage(new Page<>(page, pageSize), query);
        return PageResponse.of(
                buildResponses(result.getRecords()),
                result.getCurrent(),
                result.getSize(),
                result.getTotal());
    }

    @Transactional(readOnly = true)
    public FoodResponse getFood(Long foodId) {
        Food food = foodMapper.selectOne(
                new LambdaQueryWrapper<Food>()
                        .eq(Food::getId, foodId)
                        .eq(Food::getStatus, ACTIVE));
        if (food == null) {
            throw new BusinessException("FOOD_NOT_FOUND", "食物不存在", HttpStatus.NOT_FOUND);
        }
        return buildResponses(List.of(food)).get(0);
    }

    private void applyAllergenExclusions(LambdaQueryWrapper<Food> query, String excludeAllergens) {
        List<String> codes = splitCodes(excludeAllergens);
        if (codes.isEmpty()) {
            return;
        }

        List<Allergen> allergens = allergenMapper.selectList(
                new LambdaQueryWrapper<Allergen>().in(Allergen::getCode, codes));
        Set<String> foundCodes = allergens.stream()
                .map(Allergen::getCode)
                .map(code -> code.toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());
        List<String> unknownCodes = codes.stream()
                .filter(code -> !foundCodes.contains(code))
                .toList();
        if (!unknownCodes.isEmpty()) {
            throw new BusinessException(
                    "ALLERGEN_NOT_FOUND",
                    "过敏原不存在: " + String.join(",", unknownCodes),
                    HttpStatus.NOT_FOUND);
        }

        for (Allergen allergen : allergens) {
            query.notExists(
                    "SELECT 1 FROM food_allergen fa "
                            + "WHERE fa.food_id = food.id AND fa.allergen_id = {0}",
                    allergen.getId());
        }
    }

    private List<String> splitCodes(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(code -> !code.isEmpty())
                .map(code -> code.toUpperCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    private List<FoodResponse> buildResponses(List<Food> foods) {
        if (foods.isEmpty()) {
            return List.of();
        }

        List<Long> foodIds = foods.stream().map(Food::getId).toList();
        Map<Long, FoodCategory> categories = loadCategories(foods);
        Map<Long, FoodNutrition> nutritionByFood = loadNutrition(foodIds);
        Map<Long, FoodServing> servingByFood = loadDefaultServings(foodIds);
        Map<Long, List<String>> allergensByFood = loadAllergens(foodIds);

        return foods.stream()
                .map(food -> toResponse(
                        food,
                        categories.get(food.getCategoryId()),
                        nutritionByFood.get(food.getId()),
                        servingByFood.get(food.getId()),
                        allergensByFood.getOrDefault(food.getId(), List.of())))
                .toList();
    }

    private Map<Long, FoodCategory> loadCategories(List<Food> foods) {
        List<Long> categoryIds = foods.stream()
                .map(Food::getCategoryId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (categoryIds.isEmpty()) {
            return Map.of();
        }
        return foodCategoryMapper.selectByIds(categoryIds).stream()
                .collect(Collectors.toMap(FoodCategory::getId, Function.identity()));
    }

    private Map<Long, FoodNutrition> loadNutrition(List<Long> foodIds) {
        List<FoodNutrition> nutritionRows = foodNutritionMapper.selectList(
                new LambdaQueryWrapper<FoodNutrition>()
                        .in(FoodNutrition::getFoodId, foodIds)
                        .orderByAsc(FoodNutrition::getFoodId)
                        .orderByAsc(FoodNutrition::getId));
        Map<Long, FoodNutrition> result = new HashMap<>();
        for (FoodNutrition nutrition : nutritionRows) {
            result.merge(nutrition.getFoodId(), nutrition, (current, candidate) ->
                    isCanonical(candidate) && !isCanonical(current) ? candidate : current);
        }
        return result;
    }

    private boolean isCanonical(FoodNutrition nutrition) {
        return CANONICAL_AMOUNT.compareTo(nutrition.getServingAmount()) == 0
                && CANONICAL_UNIT.equalsIgnoreCase(nutrition.getServingUnit());
    }

    private Map<Long, FoodServing> loadDefaultServings(List<Long> foodIds) {
        List<FoodServing> servingRows = foodServingMapper.selectList(
                new LambdaQueryWrapper<FoodServing>()
                        .in(FoodServing::getFoodId, foodIds)
                        .orderByDesc(FoodServing::getIsDefault)
                        .orderByAsc(FoodServing::getId));
        Map<Long, FoodServing> result = new LinkedHashMap<>();
        for (FoodServing serving : servingRows) {
            result.putIfAbsent(serving.getFoodId(), serving);
        }
        return result;
    }

    private Map<Long, List<String>> loadAllergens(List<Long> foodIds) {
        List<FoodAllergen> links = foodAllergenMapper.selectByFoodIds(foodIds);
        if (links.isEmpty()) {
            return Map.of();
        }

        List<Long> allergenIds = links.stream()
                .map(FoodAllergen::getAllergenId)
                .distinct()
                .toList();
        Map<Long, Allergen> allergenById = allergenMapper.selectByIds(allergenIds).stream()
                .collect(Collectors.toMap(Allergen::getId, Function.identity()));
        Map<Long, List<String>> result = new LinkedHashMap<>();
        for (FoodAllergen link : links) {
            Allergen allergen = allergenById.get(link.getAllergenId());
            if (allergen != null) {
                result.computeIfAbsent(link.getFoodId(), ignored -> new ArrayList<>())
                        .add(allergen.getCode());
            }
        }
        return result;
    }

    private FoodResponse toResponse(
            Food food,
            FoodCategory category,
            FoodNutrition nutrition,
            FoodServing serving,
            List<String> allergens) {
        FoodNutritionResponse nutritionResponse = nutrition == null
                ? null
                : new FoodNutritionResponse(
                        nutrition.getCalories(),
                        nutrition.getProteinG(),
                        nutrition.getCarbohydrateG(),
                        nutrition.getFatG(),
                        nutrition.getFiberG(),
                        nutrition.getSugarG(),
                        nutrition.getSodiumMg());
        return new FoodResponse(
                food.getId(),
                food.getName(),
                category == null ? null : category.getCode(),
                serving == null ? null : serving.getAmount(),
                serving == null ? null : serving.getUnit(),
                nutritionResponse,
                allergens,
                food.getSource(),
                food.getSourceVersion());
    }
}
