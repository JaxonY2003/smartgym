package com.jaxon.smart_gym.nutrition.internal.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jaxon.smart_gym.common.exception.BusinessException;
import com.jaxon.smart_gym.common.response.ApiResponse;
import com.jaxon.smart_gym.common.response.PageResponse;
import com.jaxon.smart_gym.common.trace.TraceIdUtils;
import com.jaxon.smart_gym.nutrition.internal.dto.request.MealPlanCreateRequest;
import com.jaxon.smart_gym.nutrition.internal.dto.request.MealPlanReplaceItemRequest;
import com.jaxon.smart_gym.nutrition.internal.dto.request.MealPlanUpdateRequest;
import com.jaxon.smart_gym.nutrition.internal.dto.response.MealPlanResponse;
import com.jaxon.smart_gym.nutrition.internal.service.MealPlanService;

@RestController
@Validated
@RequestMapping("/api/v1/meal-plans")
public class MealPlanController {

    private final MealPlanService mealPlanService;

    public MealPlanController(MealPlanService mealPlanService) {
        this.mealPlanService = mealPlanService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<MealPlanResponse>>> listPlans(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须从 1 开始") long page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "每页数量必须大于 0") @Max(value = 100, message = "每页最多 100 条") long pageSize,
            HttpServletRequest request) {
        PageResponse<MealPlanResponse> data = mealPlanService.listPlans(
                userId(jwt), status, page, pageSize);
        return ResponseEntity.ok(ApiResponse.success(data, traceId(request)));
    }

    @GetMapping("/{planId}")
    public ResponseEntity<ApiResponse<MealPlanResponse>> getPlan(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @Min(value = 1, message = "计划 ID 必须大于 0") Long planId,
            HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                mealPlanService.getPlan(userId(jwt), planId), traceId(request)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MealPlanResponse>> createPlan(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody MealPlanCreateRequest requestBody,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "饮食计划创建成功",
                mealPlanService.createPlan(userId(jwt), requestBody),
                traceId(request)));
    }

    @PatchMapping("/{planId}")
    public ResponseEntity<ApiResponse<MealPlanResponse>> updatePlan(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @Min(value = 1, message = "计划 ID 必须大于 0") Long planId,
            @Valid @RequestBody MealPlanUpdateRequest requestBody,
            HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "饮食计划更新成功",
                mealPlanService.updatePlan(userId(jwt), planId, requestBody),
                traceId(request)));
    }

    @PostMapping("/{planId}/activate")
    public ResponseEntity<ApiResponse<MealPlanResponse>> activatePlan(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @Min(value = 1, message = "计划 ID 必须大于 0") Long planId,
            HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "饮食计划已激活",
                mealPlanService.activatePlan(userId(jwt), planId),
                traceId(request)));
    }

    @PostMapping("/{planId}/replace-item")
    public ResponseEntity<ApiResponse<MealPlanResponse>> replaceItem(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @Min(value = 1, message = "计划 ID 必须大于 0") Long planId,
            @Valid @RequestBody MealPlanReplaceItemRequest requestBody,
            HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "计划食物已替换",
                mealPlanService.replaceItem(userId(jwt), planId, requestBody),
                traceId(request)));
    }

    private Long userId(Jwt jwt) {
        try {
            return Long.valueOf(jwt.getSubject());
        } catch (RuntimeException exception) {
            throw new BusinessException(
                    "AUTH_TOKEN_INVALID",
                    "登录状态无效，请重新登录",
                    HttpStatus.UNAUTHORIZED);
        }
    }

    private String traceId(HttpServletRequest request) {
        return TraceIdUtils.get(request);
    }
}
