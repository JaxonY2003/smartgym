package com.jaxon.smart_gym.nutrition.internal.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jaxon.smart_gym.common.response.ApiResponse;
import com.jaxon.smart_gym.common.response.PageResponse;
import com.jaxon.smart_gym.common.trace.TraceIdUtils;
import com.jaxon.smart_gym.nutrition.internal.dto.response.FoodResponse;
import com.jaxon.smart_gym.nutrition.internal.service.FoodCatalogService;

@RestController
@Validated
@RequestMapping("/api/v1")
public class FoodController {

    private final FoodCatalogService foodCatalogService;

    public FoodController(FoodCatalogService foodCatalogService) {
        this.foodCatalogService = foodCatalogService;
    }

    @GetMapping("/foods")
    public ResponseEntity<ApiResponse<PageResponse<FoodResponse>>> listFoods(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String excludeAllergens,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须从 1 开始") long page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "每页数量必须大于 0") @Max(value = 100, message = "每页最多 100 条") long pageSize,
            HttpServletRequest request) {
        PageResponse<FoodResponse> data = foodCatalogService.listFoods(
                keyword, category, excludeAllergens, page, pageSize);
        return ResponseEntity.ok(ApiResponse.success(data, traceId(request)));
    }

    @GetMapping("/foods/{foodId}")
    public ResponseEntity<ApiResponse<FoodResponse>> getFood(
            @PathVariable @Min(value = 1, message = "食物 ID 必须大于 0") Long foodId,
            HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                foodCatalogService.getFood(foodId), traceId(request)));
    }

    private String traceId(HttpServletRequest request) {
        return TraceIdUtils.get(request);
    }
}
