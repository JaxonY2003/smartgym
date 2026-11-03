package com.jaxon.smart_gym.training.internal.controller;

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
import com.jaxon.smart_gym.training.internal.dto.response.EquipmentResponse;
import com.jaxon.smart_gym.training.internal.dto.response.ExerciseDetailResponse;
import com.jaxon.smart_gym.training.internal.dto.response.ExerciseListResponse;
import com.jaxon.smart_gym.training.internal.service.TrainingCatalogService;

@RestController
@Validated
@RequestMapping("/api/v1")
public class TrainingCatalogController {

    private final TrainingCatalogService trainingCatalogService;

    public TrainingCatalogController(TrainingCatalogService trainingCatalogService) {
        this.trainingCatalogService = trainingCatalogService;
    }

    @GetMapping("/exercises")
    public ResponseEntity<ApiResponse<PageResponse<ExerciseListResponse>>> listExercises(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String muscleGroup,
            @RequestParam(required = false) @Min(value = 1, message = "器械 ID 必须大于 0") Long equipmentId,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须从 1 开始") long page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "每页数量必须大于 0") @Max(value = 100, message = "每页最多 100 条") long pageSize,
            HttpServletRequest request) {
        PageResponse<ExerciseListResponse> data = trainingCatalogService.listExercises(
                keyword, muscleGroup, equipmentId, page, pageSize);
        return ResponseEntity.ok(ApiResponse.success(data, traceId(request)));
    }

    @GetMapping("/exercises/{exerciseId}")
    public ResponseEntity<ApiResponse<ExerciseDetailResponse>> getExercise(
            @PathVariable @Min(value = 1, message = "动作 ID 必须大于 0") Long exerciseId,
            HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                trainingCatalogService.getExercise(exerciseId), traceId(request)));
    }

    @GetMapping("/equipment")
    public ResponseEntity<ApiResponse<PageResponse<EquipmentResponse>>> listEquipment(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须从 1 开始") long page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "每页数量必须大于 0") @Max(value = 100, message = "每页最多 100 条") long pageSize,
            HttpServletRequest request) {
        PageResponse<EquipmentResponse> data = trainingCatalogService.listEquipment(
                status, page, pageSize);
        return ResponseEntity.ok(ApiResponse.success(data, traceId(request)));
    }

    @GetMapping("/equipment/{equipmentId}")
    public ResponseEntity<ApiResponse<EquipmentResponse>> getEquipment(
            @PathVariable @Min(value = 1, message = "器械 ID 必须大于 0") Long equipmentId,
            HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                trainingCatalogService.getEquipment(equipmentId), traceId(request)));
    }

    private String traceId(HttpServletRequest request) {
        return TraceIdUtils.get(request);
    }
}
