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
import org.springframework.web.bind.annotation.GetMapping;
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
import com.jaxon.smart_gym.workout.internal.dto.request.WorkoutFeedbackRequest;
import com.jaxon.smart_gym.workout.internal.dto.request.WorkoutSessionCreateRequest;
import com.jaxon.smart_gym.workout.internal.dto.response.WorkoutSessionResponse;
import com.jaxon.smart_gym.workout.internal.service.WorkoutService;

@RestController
@Validated
@RequestMapping("/api/v1/workout-sessions")
public class WorkoutSessionController {

    private final WorkoutService workoutService;

    public WorkoutSessionController(WorkoutService workoutService) {
        this.workoutService = workoutService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WorkoutSessionResponse>> createSession(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody WorkoutSessionCreateRequest requestBody,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "训练记录创建成功",
                workoutService.createSession(userId(jwt), requestBody),
                traceId(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<WorkoutSessionResponse>>> listSessions(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须从 1 开始") long page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "每页数量必须大于 0") @Max(value = 100, message = "每页最多 100 条") long pageSize,
            HttpServletRequest request) {
        PageResponse<WorkoutSessionResponse> data = workoutService.listSessions(
                userId(jwt), from, to, status, page, pageSize);
        return ResponseEntity.ok(ApiResponse.success(data, traceId(request)));
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<ApiResponse<WorkoutSessionResponse>> getSession(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @Min(value = 1, message = "训练记录 ID 必须大于 0") Long sessionId,
            HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                workoutService.getSession(userId(jwt), sessionId), traceId(request)));
    }

    @PostMapping("/{sessionId}/feedback")
    public ResponseEntity<ApiResponse<WorkoutSessionResponse>> addFeedback(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @Min(value = 1, message = "训练记录 ID 必须大于 0") Long sessionId,
            @Valid @RequestBody WorkoutFeedbackRequest requestBody,
            HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "训练反馈已保存",
                workoutService.addFeedback(userId(jwt), sessionId, requestBody),
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
