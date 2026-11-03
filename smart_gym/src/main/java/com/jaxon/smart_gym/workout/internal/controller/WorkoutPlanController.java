package com.jaxon.smart_gym.workout.internal.controller;

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
import org.springframework.web.bind.annotation.DeleteMapping;
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
import com.jaxon.smart_gym.workout.internal.dto.request.WorkoutPlanCreateRequest;
import com.jaxon.smart_gym.workout.internal.dto.request.WorkoutPlanUpdateRequest;
import com.jaxon.smart_gym.workout.internal.dto.response.WorkoutPlanResponse;
import com.jaxon.smart_gym.workout.internal.service.WorkoutService;

@RestController
@Validated
@RequestMapping("/api/v1/workout-plans")
public class WorkoutPlanController {

    private final WorkoutService workoutService;

    public WorkoutPlanController(WorkoutService workoutService) {
        this.workoutService = workoutService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<WorkoutPlanResponse>>> listPlans(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须从 1 开始") long page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "每页数量必须大于 0") @Max(value = 100, message = "每页最多 100 条") long pageSize,
            HttpServletRequest request) {
        PageResponse<WorkoutPlanResponse> data = workoutService.listPlans(
                userId(jwt), status, page, pageSize);
        return ResponseEntity.ok(ApiResponse.success(data, traceId(request)));
    }

    @GetMapping("/today")
    public ResponseEntity<ApiResponse<WorkoutPlanResponse>> getTodayPlan(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) LocalDate date,
            HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                workoutService.getTodayPlan(userId(jwt), date), traceId(request)));
    }

    @GetMapping("/{planId}")
    public ResponseEntity<ApiResponse<WorkoutPlanResponse>> getPlan(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @Min(value = 1, message = "计划 ID 必须大于 0") Long planId,
            HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                workoutService.getPlan(userId(jwt), planId), traceId(request)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WorkoutPlanResponse>> createPlan(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody WorkoutPlanCreateRequest requestBody,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "训练计划创建成功",
                workoutService.createPlan(userId(jwt), requestBody),
                traceId(request)));
    }

    @PatchMapping("/{planId}")
    public ResponseEntity<ApiResponse<WorkoutPlanResponse>> updatePlan(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @Min(value = 1, message = "计划 ID 必须大于 0") Long planId,
            @Valid @RequestBody WorkoutPlanUpdateRequest requestBody,
            HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "训练计划更新成功",
                workoutService.updatePlan(userId(jwt), planId, requestBody),
                traceId(request)));
    }

    @PostMapping("/{planId}/activate")
    public ResponseEntity<ApiResponse<WorkoutPlanResponse>> activatePlan(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @Min(value = 1, message = "计划 ID 必须大于 0") Long planId,
            HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "训练计划已激活",
                workoutService.activatePlan(userId(jwt), planId),
                traceId(request)));
    }

    @DeleteMapping("/{planId}")
    public ResponseEntity<Void> deletePlan(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @Min(value = 1, message = "计划 ID 必须大于 0") Long planId) {
        workoutService.deletePlan(userId(jwt), planId);
        return ResponseEntity.noContent().build();
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
