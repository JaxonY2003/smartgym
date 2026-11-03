package com.jaxon.smart_gym.dashboard.internal.controller;

import java.time.LocalDate;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jaxon.smart_gym.common.exception.BusinessException;
import com.jaxon.smart_gym.common.response.ApiResponse;
import com.jaxon.smart_gym.common.trace.TraceIdUtils;
import com.jaxon.smart_gym.dashboard.internal.dto.response.DashboardOverviewResponse;
import com.jaxon.smart_gym.dashboard.internal.dto.response.DashboardTrendResponse;
import com.jaxon.smart_gym.dashboard.internal.service.DashboardService;

@RestController
@Validated
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<DashboardOverviewResponse>> getOverview(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) LocalDate date,
            HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getOverview(userId(jwt), date), traceId(request)));
    }

    @GetMapping("/trends")
    public ResponseEntity<ApiResponse<DashboardTrendResponse>> getTrend(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam String metric,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getTrend(userId(jwt), from, to, metric), traceId(request)));
    }

    private Long userId(Jwt jwt) {
        try {
            return Long.valueOf(jwt.getSubject());
        } catch (RuntimeException exception) {
            throw new BusinessException(
                    "AUTH_TOKEN_INVALID",
                    "登录状态无效，请重新登录",
                    org.springframework.http.HttpStatus.UNAUTHORIZED);
        }
    }

    private String traceId(HttpServletRequest request) {
        return TraceIdUtils.get(request);
    }
}
