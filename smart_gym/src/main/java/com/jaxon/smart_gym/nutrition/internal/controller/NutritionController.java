package com.jaxon.smart_gym.nutrition.internal.controller;

import java.time.LocalDate;

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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jaxon.smart_gym.common.exception.BusinessException;
import com.jaxon.smart_gym.common.response.ApiResponse;
import com.jaxon.smart_gym.common.response.PageResponse;
import com.jaxon.smart_gym.common.trace.TraceIdUtils;
import com.jaxon.smart_gym.nutrition.internal.dto.request.MealRecordCreateRequest;
import com.jaxon.smart_gym.nutrition.internal.dto.request.NutritionTargetRequest;
import com.jaxon.smart_gym.nutrition.internal.dto.request.WaterRecordCreateRequest;
import com.jaxon.smart_gym.nutrition.internal.dto.response.DailyNutritionSummaryResponse;
import com.jaxon.smart_gym.nutrition.internal.dto.response.MealRecordResponse;
import com.jaxon.smart_gym.nutrition.internal.dto.response.NutritionTargetResponse;
import com.jaxon.smart_gym.nutrition.internal.dto.response.NutritionTrendResponse;
import com.jaxon.smart_gym.nutrition.internal.dto.response.WaterRecordResponse;
import com.jaxon.smart_gym.nutrition.internal.service.NutritionService;

@RestController
@Validated
@RequestMapping("/api/v1")
public class NutritionController {

    private final NutritionService nutritionService;

    public NutritionController(NutritionService nutritionService) {
        this.nutritionService = nutritionService;
    }

    @GetMapping("/nutrition-targets/current")
    public ResponseEntity<ApiResponse<NutritionTargetResponse>> getCurrentTarget(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                nutritionService.getCurrentTarget(userId(jwt)), traceId(request)));
    }

    @PutMapping("/nutrition-targets/current")
    public ResponseEntity<ApiResponse<NutritionTargetResponse>> updateCurrentTarget(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody NutritionTargetRequest requestBody,
            HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "营养目标已更新",
                nutritionService.updateCurrentTarget(userId(jwt), requestBody),
                traceId(request)));
    }

    @PostMapping("/meal-records")
    public ResponseEntity<ApiResponse<MealRecordResponse>> createMealRecord(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody MealRecordCreateRequest requestBody,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "饮食记录创建成功",
                nutritionService.createMealRecord(userId(jwt), requestBody),
                traceId(request)));
    }

    @GetMapping("/meal-records")
    public ResponseEntity<ApiResponse<PageResponse<MealRecordResponse>>> listMealRecords(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) String mealType,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须从 1 开始") long page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "每页数量必须大于 0") @Max(value = 100, message = "每页最多 100 条") long pageSize,
            HttpServletRequest request) {
        PageResponse<MealRecordResponse> data = nutritionService.listMealRecords(
                userId(jwt), date, mealType, page, pageSize);
        return ResponseEntity.ok(ApiResponse.success(data, traceId(request)));
    }

    @GetMapping("/nutrition/daily-summary")
    public ResponseEntity<ApiResponse<DailyNutritionSummaryResponse>> getDailySummary(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) LocalDate date,
            HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                nutritionService.getDailySummary(userId(jwt), date), traceId(request)));
    }

    @GetMapping("/nutrition/trends")
    public ResponseEntity<ApiResponse<NutritionTrendResponse>> getNutritionTrend(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @RequestParam(required = false, defaultValue = "CALORIES") String metric,
            HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                nutritionService.getNutritionTrend(userId(jwt), from, to, metric), traceId(request)));
    }

    @PostMapping("/water-records")
    public ResponseEntity<ApiResponse<WaterRecordResponse>> createWaterRecord(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody WaterRecordCreateRequest requestBody,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "饮水记录创建成功",
                nutritionService.createWaterRecord(userId(jwt), requestBody),
                traceId(request)));
    }

    @GetMapping("/water-records")
    public ResponseEntity<ApiResponse<java.util.List<WaterRecordResponse>>> listWaterRecords(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam LocalDate date,
            HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                nutritionService.listWaterRecords(userId(jwt), date), traceId(request)));
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
