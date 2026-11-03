package com.jaxon.smart_gym.training.internal.controller;

import java.util.Collection;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jaxon.smart_gym.common.response.ApiResponse;
import com.jaxon.smart_gym.common.response.PageResponse;
import com.jaxon.smart_gym.common.trace.TraceIdUtils;
import com.jaxon.smart_gym.training.internal.dto.response.MuscleGroupResponse;
import com.jaxon.smart_gym.training.internal.service.TrainingCatalogService;

@RestController
@Validated
@RequestMapping("/api/v1")
public class MuscleGroupController {

    private final TrainingCatalogService trainingCatalogService;

    public MuscleGroupController(TrainingCatalogService trainingCatalogService) {
        this.trainingCatalogService = trainingCatalogService;
    }

    @GetMapping("/muscle-groups")
    public ResponseEntity<ApiResponse<PageResponse<MuscleGroupResponse>>> listMuscleGroups(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @Min(value = 1, message = "父级肌群 ID 必须大于 0") Long parentId,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须从 1 开始") long page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "每页数量必须大于 0") @Max(value = 100, message = "每页最多 100 条") long pageSize,
            Authentication authentication,
            HttpServletRequest request) {
        boolean administrator = isAdministrator(authentication);
        PageResponse<MuscleGroupResponse> data = trainingCatalogService.listMuscleGroups(
                status, parentId, page, pageSize, administrator);
        return ResponseEntity.ok(ApiResponse.success(data, TraceIdUtils.get(request)));
    }

    @GetMapping("/muscle-groups/{muscleGroupId}")
    public ResponseEntity<ApiResponse<MuscleGroupResponse>> getMuscleGroup(
            @PathVariable @Min(value = 1, message = "肌群 ID 必须大于 0") Long muscleGroupId,
            Authentication authentication,
            HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                trainingCatalogService.getMuscleGroup(muscleGroupId, isAdministrator(authentication)),
                TraceIdUtils.get(request)));
    }

    private boolean isAdministrator(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        Collection<?> authorities = authentication.getAuthorities();
        return authorities.stream().anyMatch(authority -> "ROLE_ADMIN".equals(authority.toString()));
    }
}
