package com.jaxon.smart_gym.profile.internal.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jaxon.smart_gym.common.exception.BusinessException;
import com.jaxon.smart_gym.common.response.ApiResponse;
import com.jaxon.smart_gym.common.response.PageResponse;
import com.jaxon.smart_gym.common.trace.TraceIdUtils;
import com.jaxon.smart_gym.profile.internal.dto.BodyMetricCommand;
import com.jaxon.smart_gym.profile.internal.dto.HealthConstraintCommand;
import com.jaxon.smart_gym.profile.internal.dto.MemberProfileCommand;
import com.jaxon.smart_gym.profile.internal.dto.MemberProfileView;
import com.jaxon.smart_gym.profile.internal.dto.NutritionProfileCommand;
import com.jaxon.smart_gym.profile.internal.dto.NutritionProfileView;
import com.jaxon.smart_gym.profile.internal.dto.request.BodyMetricRequest;
import com.jaxon.smart_gym.profile.internal.dto.request.HealthConstraintRequest;
import com.jaxon.smart_gym.profile.internal.dto.request.MemberProfileRequest;
import com.jaxon.smart_gym.profile.internal.dto.request.NutritionProfileRequest;
import com.jaxon.smart_gym.profile.internal.dto.response.BodyMetricResponse;
import com.jaxon.smart_gym.profile.internal.dto.response.HealthConstraintResponse;
import com.jaxon.smart_gym.profile.internal.dto.response.MemberProfileResponse;
import com.jaxon.smart_gym.profile.internal.dto.response.NutritionProfileResponse;
import com.jaxon.smart_gym.profile.internal.entity.BodyMetric;
import com.jaxon.smart_gym.profile.internal.entity.HealthConstraint;
import com.jaxon.smart_gym.profile.internal.service.HealthProfileService;

@RestController
@Validated
@RequestMapping("/api/v1/member")
public class MemberProfileController {

    private final HealthProfileService healthProfileService;

    public MemberProfileController(HealthProfileService healthProfileService) {
        this.healthProfileService = healthProfileService;
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<MemberProfileResponse>> getProfile(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {
        MemberProfileView profile = healthProfileService.getProfile(userId(jwt));
        if (profile == null) {
            throw notFound("健康档案不存在");
        }
        return ResponseEntity.ok(ApiResponse.success(toResponse(profile), traceId(request)));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<MemberProfileResponse>> saveProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody MemberProfileRequest request,
            HttpServletRequest httpRequest) {
        MemberProfileCommand command = new MemberProfileCommand(
                request.gender(),
                request.birthDate(),
                request.heightCm(),
                request.weightKg(),
                request.trainingExperience(),
                request.weeklyTrainingDays(),
                request.maxSessionMinutes(),
                request.goals(),
                request.medicalNotice(),
                request.note());
        MemberProfileView profile = healthProfileService.saveProfile(userId(jwt), command);
        return ResponseEntity.ok(ApiResponse.success(toResponse(profile), traceId(httpRequest)));
    }

    @GetMapping("/body-metrics")
    public ResponseEntity<ApiResponse<PageResponse<BodyMetricResponse>>> listBodyMetrics(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须从 1 开始") long page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "每页数量必须大于 0") @Max(value = 100, message = "每页最多 100 条") long pageSize,
            HttpServletRequest request) {
        LocalDateTime fromTime = from == null ? null : from.atStartOfDay();
        LocalDateTime toTime = to == null ? null : to.plusDays(1).atStartOfDay();
        Page<BodyMetric> result = healthProfileService.listBodyMetrics(
                userId(jwt), fromTime, toTime, page, pageSize);
        List<BodyMetricResponse> items = result.getRecords().stream()
                .map(this::toResponse)
                .toList();
        PageResponse<BodyMetricResponse> data = PageResponse.of(
                items, result.getCurrent(), result.getSize(), result.getTotal());
        return ResponseEntity.ok(ApiResponse.success(data, traceId(request)));
    }

    @PostMapping("/body-metrics")
    public ResponseEntity<ApiResponse<BodyMetricResponse>> addBodyMetric(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody BodyMetricRequest request,
            HttpServletRequest httpRequest) {
        BodyMetric metric = healthProfileService.addBodyMetric(
                userId(jwt),
                new BodyMetricCommand(
                        request.measuredAt(),
                        request.weightKg(),
                        request.bodyFatPercent(),
                        request.waistCm(),
                        request.note()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(toResponse(metric), traceId(httpRequest)));
    }

    @GetMapping("/health-constraints")
    public ResponseEntity<ApiResponse<List<HealthConstraintResponse>>> listHealthConstraints(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {
        List<HealthConstraintResponse> data = healthProfileService
                .listHealthConstraints(userId(jwt))
                .stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(data, traceId(request)));
    }

    @PutMapping("/health-constraints")
    public ResponseEntity<ApiResponse<List<HealthConstraintResponse>>> replaceHealthConstraints(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody HealthConstraintRequest request,
            HttpServletRequest httpRequest) {
        List<HealthConstraintCommand> commands = request.constraints().stream()
                .map(item -> new HealthConstraintCommand(
                        item.type(),
                        item.bodyPart(),
                        item.severity(),
                        item.description(),
                        item.active(),
                        item.startedOn(),
                        item.endedOn()))
                .toList();
        List<HealthConstraintResponse> data = healthProfileService
                .replaceHealthConstraints(userId(jwt), commands, request.medicalNotice())
                .stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(data, traceId(httpRequest)));
    }

    @GetMapping("/nutrition-profile")
    public ResponseEntity<ApiResponse<NutritionProfileResponse>> getNutritionProfile(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {
        NutritionProfileView profile = healthProfileService.getNutritionProfile(userId(jwt));
        if (profile == null) {
            throw notFound("饮食档案不存在");
        }
        return ResponseEntity.ok(ApiResponse.success(toResponse(profile), traceId(request)));
    }

    @PutMapping("/nutrition-profile")
    public ResponseEntity<ApiResponse<NutritionProfileResponse>> saveNutritionProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody NutritionProfileRequest request,
            HttpServletRequest httpRequest) {
        NutritionProfileView profile = healthProfileService.saveNutritionProfile(
                userId(jwt),
                new NutritionProfileCommand(
                        request.dietType(),
                        request.allergies(),
                        request.restrictions(),
                        request.dislikedFoods(),
                        request.preferredCuisines(),
                        request.dailyWaterMlTarget(),
                        request.note()));
        return ResponseEntity.ok(ApiResponse.success(toResponse(profile), traceId(httpRequest)));
    }

    private MemberProfileResponse toResponse(MemberProfileView profile) {
        return new MemberProfileResponse(
                profile.userId(),
                profile.gender(),
                profile.birthDate(),
                profile.heightCm(),
                profile.weightKg(),
                profile.trainingExperience(),
                profile.weeklyTrainingDays(),
                profile.maxSessionMinutes(),
                profile.goals(),
                profile.medicalNotice(),
                profile.note(),
                profile.updatedAt());
    }

    private BodyMetricResponse toResponse(BodyMetric metric) {
        return new BodyMetricResponse(
                metric.getId(),
                toUtcOffset(metric.getMeasuredAt()),
                metric.getWeightKg(),
                metric.getBodyFatPercent(),
                metric.getWaistCm(),
                metric.getNote());
    }

    private HealthConstraintResponse toResponse(HealthConstraint constraint) {
        return new HealthConstraintResponse(
                constraint.getId(),
                constraint.getConstraintType(),
                constraint.getBodyPart(),
                constraint.getSeverity(),
                constraint.getDescription(),
                constraint.getActive(),
                constraint.getStartedOn(),
                constraint.getEndedOn());
    }

    private NutritionProfileResponse toResponse(NutritionProfileView profile) {
        return new NutritionProfileResponse(
                profile.userId(),
                profile.dietType(),
                profile.allergies(),
                profile.restrictions(),
                profile.dislikedFoods(),
                profile.preferredCuisines(),
                profile.dailyWaterMlTarget(),
                profile.note(),
                profile.updatedAt());
    }

    private OffsetDateTime toUtcOffset(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
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

    private BusinessException notFound(String message) {
        return new BusinessException(
                "RESOURCE_NOT_FOUND",
                message,
                HttpStatus.NOT_FOUND);
    }

    private String traceId(HttpServletRequest request) {
        return TraceIdUtils.get(request);
    }
}
