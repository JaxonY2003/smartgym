package com.jaxon.smart_gym.training.internal.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import com.jaxon.smart_gym.common.exception.BusinessException;
import com.jaxon.smart_gym.common.response.PageResponse;
import com.jaxon.smart_gym.training.internal.dto.response.EquipmentResponse;
import com.jaxon.smart_gym.training.internal.dto.response.EquipmentSummaryResponse;
import com.jaxon.smart_gym.training.internal.dto.response.ExerciseDetailResponse;
import com.jaxon.smart_gym.training.internal.dto.response.ExerciseListResponse;
import com.jaxon.smart_gym.training.internal.dto.response.MuscleGroupResponse;
import com.jaxon.smart_gym.training.internal.entity.Equipment;
import com.jaxon.smart_gym.training.internal.entity.Exercise;
import com.jaxon.smart_gym.training.internal.entity.ExerciseEquipment;
import com.jaxon.smart_gym.training.internal.entity.ExerciseMuscle;
import com.jaxon.smart_gym.training.internal.entity.MuscleGroup;
import com.jaxon.smart_gym.training.internal.mapper.EquipmentMapper;
import com.jaxon.smart_gym.training.internal.mapper.ExerciseEquipmentMapper;
import com.jaxon.smart_gym.training.internal.mapper.ExerciseMapper;
import com.jaxon.smart_gym.training.internal.mapper.ExerciseMuscleMapper;
import com.jaxon.smart_gym.training.internal.mapper.MuscleGroupMapper;

@Service
public class TrainingCatalogService {

    private static final String ACTIVE = "ACTIVE";

    private final ExerciseMapper exerciseMapper;
    private final MuscleGroupMapper muscleGroupMapper;
    private final ExerciseMuscleMapper exerciseMuscleMapper;
    private final EquipmentMapper equipmentMapper;
    private final ExerciseEquipmentMapper exerciseEquipmentMapper;
    private final JsonMapper objectMapper;

    public TrainingCatalogService(
            ExerciseMapper exerciseMapper,
            MuscleGroupMapper muscleGroupMapper,
            ExerciseMuscleMapper exerciseMuscleMapper,
            EquipmentMapper equipmentMapper,
            ExerciseEquipmentMapper exerciseEquipmentMapper,
            JsonMapper objectMapper) {
        this.exerciseMapper = exerciseMapper;
        this.muscleGroupMapper = muscleGroupMapper;
        this.exerciseMuscleMapper = exerciseMuscleMapper;
        this.equipmentMapper = equipmentMapper;
        this.exerciseEquipmentMapper = exerciseEquipmentMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<MuscleGroupResponse> listMuscleGroups(
            String status,
            Long parentId,
            long page,
            long pageSize,
            boolean administrator) {
        String normalizedStatus = normalizeMuscleGroupStatus(status, administrator);
        LambdaQueryWrapper<MuscleGroup> query = new LambdaQueryWrapper<MuscleGroup>()
                .eq(MuscleGroup::getStatus, normalizedStatus)
                .orderByAsc(MuscleGroup::getSortOrder)
                .orderByAsc(MuscleGroup::getId);
        if (parentId != null) {
            query.eq(MuscleGroup::getParentId, parentId);
        }

        Page<MuscleGroup> result = muscleGroupMapper.selectPage(
                new Page<>(page, pageSize), query);
        List<MuscleGroupResponse> items = result.getRecords().stream()
                .map(this::toMuscleGroupResponse)
                .toList();
        return PageResponse.of(items, result.getCurrent(), result.getSize(), result.getTotal());
    }

    @Transactional(readOnly = true)
    public MuscleGroupResponse getMuscleGroup(Long muscleGroupId, boolean administrator) {
        LambdaQueryWrapper<MuscleGroup> query = new LambdaQueryWrapper<MuscleGroup>()
                .eq(MuscleGroup::getId, muscleGroupId);
        if (!administrator) {
            query.eq(MuscleGroup::getStatus, ACTIVE);
        }
        MuscleGroup muscleGroup = muscleGroupMapper.selectOne(query);
        if (muscleGroup == null) {
            throw new BusinessException(
                    "MUSCLE_GROUP_NOT_FOUND",
                    "肌群不存在",
                    HttpStatus.NOT_FOUND);
        }
        return toMuscleGroupResponse(muscleGroup);
    }

    private String normalizeMuscleGroupStatus(String status, boolean administrator) {
        if (status == null || status.isBlank()) {
            return ACTIVE;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!"ACTIVE".equals(normalized) && !"INACTIVE".equals(normalized)) {
            throw new BusinessException(
                    "VALIDATION_ERROR",
                    "肌群状态只能是 ACTIVE 或 INACTIVE",
                    HttpStatus.BAD_REQUEST);
        }
        if (!administrator && !ACTIVE.equals(normalized)) {
            throw new BusinessException(
                    "FORBIDDEN",
                    "普通用户只能查询启用中的肌群",
                    HttpStatus.FORBIDDEN);
        }
        return normalized;
    }

    private MuscleGroupResponse toMuscleGroupResponse(MuscleGroup muscleGroup) {
        return new MuscleGroupResponse(
                muscleGroup.getId(),
                muscleGroup.getCode(),
                muscleGroup.getName(),
                muscleGroup.getParentId(),
                muscleGroup.getSortOrder(),
                muscleGroup.getStatus());
    }

    @Transactional(readOnly = true)
    public PageResponse<ExerciseListResponse> listExercises(
            String keyword,
            String muscleGroup,
            Long equipmentId,
            long page,
            long pageSize) {
        LambdaQueryWrapper<Exercise> query = new LambdaQueryWrapper<Exercise>()
                .eq(Exercise::getStatus, ACTIVE)
                .orderByAsc(Exercise::getName)
                .orderByAsc(Exercise::getId);

        if (keyword != null && !keyword.isBlank()) {
            query.and(wrapper -> wrapper
                    .like(Exercise::getName, keyword.trim())
                    .or()
                    .like(Exercise::getEnglishName, keyword.trim()));
        }
        if (muscleGroup != null && !muscleGroup.isBlank()) {
            query.apply(
                    "EXISTS (SELECT 1 FROM exercise_muscle em "
                            + "INNER JOIN muscle_group mg ON mg.id = em.muscle_group_id "
                            + "WHERE em.exercise_id = exercise.id AND mg.code = {0})",
                    muscleGroup.trim().toUpperCase(Locale.ROOT));
        }
        if (equipmentId != null) {
            query.apply(
                    "EXISTS (SELECT 1 FROM exercise_equipment ee "
                            + "WHERE ee.exercise_id = exercise.id AND ee.equipment_id = {0})",
                    equipmentId);
        }

        Page<Exercise> result = exerciseMapper.selectPage(new Page<>(page, pageSize), query);
        AssociationData associations = loadAssociations(result.getRecords());
        List<ExerciseListResponse> items = result.getRecords().stream()
                .map(exercise -> toListResponse(exercise, associations))
                .toList();
        return PageResponse.of(items, result.getCurrent(), result.getSize(), result.getTotal());
    }

    @Transactional(readOnly = true)
    public ExerciseDetailResponse getExercise(Long exerciseId) {
        Exercise exercise = exerciseMapper.selectOne(
                new LambdaQueryWrapper<Exercise>()
                        .eq(Exercise::getId, exerciseId)
                        .eq(Exercise::getStatus, ACTIVE));
        if (exercise == null) {
            throw new BusinessException(
                    "EXERCISE_NOT_FOUND",
                    "训练动作不存在",
                    HttpStatus.NOT_FOUND);
        }

        AssociationData associations = loadAssociations(List.of(exercise));
        List<String> primaryMuscles = associations.musclesByExercise()
                .getOrDefault(exercise.getId(), List.of());
        List<EquipmentSummaryResponse> equipment = associations.equipmentByExercise()
                .getOrDefault(exercise.getId(), List.of());
        return new ExerciseDetailResponse(
                exercise.getId(),
                exercise.getName(),
                exercise.getEnglishName(),
                exercise.getDescription(),
                exercise.getInstructions(),
                primaryMuscles,
                equipment,
                exercise.getDifficulty(),
                exercise.getDefaultRestSeconds(),
                parseContraindications(exercise.getContraindicationsJson()),
                exercise.getThumbnailUrl());
    }

    @Transactional(readOnly = true)
    public PageResponse<EquipmentResponse> listEquipment(
            String status,
            long page,
            long pageSize) {
        LambdaQueryWrapper<Equipment> query = new LambdaQueryWrapper<Equipment>()
                .orderByAsc(Equipment::getName)
                .orderByAsc(Equipment::getId);
        if (status != null && !status.isBlank()) {
            query.eq(Equipment::getStatus, status.trim().toUpperCase(Locale.ROOT));
        }

        Page<Equipment> result = equipmentMapper.selectPage(
                new Page<>(page, pageSize), query);
        List<EquipmentResponse> items = result.getRecords().stream()
                .map(this::toEquipmentResponse)
                .toList();
        return PageResponse.of(items, result.getCurrent(), result.getSize(), result.getTotal());
    }

    @Transactional(readOnly = true)
    public EquipmentResponse getEquipment(Long equipmentId) {
        Equipment equipment = equipmentMapper.selectById(equipmentId);
        if (equipment == null) {
            throw new BusinessException(
                    "EQUIPMENT_NOT_FOUND",
                    "器械不存在",
                    HttpStatus.NOT_FOUND);
        }
        return toEquipmentResponse(equipment);
    }

    private AssociationData loadAssociations(List<Exercise> exercises) {
        if (exercises.isEmpty()) {
            return new AssociationData(Map.of(), Map.of());
        }

        List<Long> exerciseIds = exercises.stream().map(Exercise::getId).toList();
        List<ExerciseMuscle> muscleLinks = exerciseMuscleMapper.selectList(
                new LambdaQueryWrapper<ExerciseMuscle>()
                        .in(ExerciseMuscle::getExerciseId, exerciseIds)
                        .orderByAsc(ExerciseMuscle::getId));
        Map<Long, MuscleGroup> muscleGroups = loadMuscleGroups(muscleLinks);
        Map<Long, List<String>> musclesByExercise = new LinkedHashMap<>();
        for (ExerciseMuscle link : muscleLinks) {
            MuscleGroup group = muscleGroups.get(link.getMuscleGroupId());
            if (group != null
                    && ACTIVE.equals(group.getStatus())
                    && "PRIMARY".equalsIgnoreCase(link.getMuscleRole())) {
                musclesByExercise.computeIfAbsent(link.getExerciseId(), ignored -> new ArrayList<>())
                        .add(group.getCode());
            }
        }

        List<ExerciseEquipment> equipmentLinks = exerciseEquipmentMapper.selectList(
                new LambdaQueryWrapper<ExerciseEquipment>()
                        .in(ExerciseEquipment::getExerciseId, exerciseIds)
                        .orderByAsc(ExerciseEquipment::getId));
        Map<Long, Equipment> equipmentById = loadEquipmentTypesFromLinks(equipmentLinks);
        Map<Long, List<EquipmentSummaryResponse>> equipmentByExercise = new LinkedHashMap<>();
        for (ExerciseEquipment link : equipmentLinks) {
            Equipment equipment = equipmentById.get(link.getEquipmentId());
            if (equipment != null && ACTIVE.equals(equipment.getStatus())) {
                equipmentByExercise.computeIfAbsent(link.getExerciseId(), ignored -> new ArrayList<>())
                        .add(new EquipmentSummaryResponse(equipment.getId(), equipment.getName()));
            }
        }
        return new AssociationData(musclesByExercise, equipmentByExercise);
    }

    private Map<Long, MuscleGroup> loadMuscleGroups(List<ExerciseMuscle> links) {
        List<Long> ids = links.stream().map(ExerciseMuscle::getMuscleGroupId).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return muscleGroupMapper.selectByIds(ids).stream()
                .collect(java.util.stream.Collectors.toMap(MuscleGroup::getId, group -> group));
    }

    private Map<Long, Equipment> loadEquipmentTypesFromLinks(List<ExerciseEquipment> links) {
        List<Long> ids = links.stream().map(ExerciseEquipment::getEquipmentId).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return equipmentMapper.selectByIds(ids).stream()
                .collect(java.util.stream.Collectors.toMap(Equipment::getId, equipment -> equipment));
    }

    private ExerciseListResponse toListResponse(Exercise exercise, AssociationData associations) {
        return new ExerciseListResponse(
                exercise.getId(),
                exercise.getName(),
                exercise.getEnglishName(),
                associations.musclesByExercise().getOrDefault(exercise.getId(), List.of()),
                associations.equipmentByExercise().getOrDefault(exercise.getId(), List.of()),
                exercise.getDifficulty(),
                exercise.getDefaultRestSeconds(),
                parseContraindications(exercise.getContraindicationsJson()),
                exercise.getThumbnailUrl());
    }

    private EquipmentResponse toEquipmentResponse(Equipment equipment) {
        return new EquipmentResponse(
                equipment.getId(),
                equipment.getName(),
                equipment.getCategory(),
                equipment.getStatus(),
                equipment.getBrand(),
                equipment.getDescription());
    }

    private List<String> parseContraindications(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() { });
        } catch (RuntimeException exception) {
            return Collections.emptyList();
        }
    }

    private record AssociationData(
            Map<Long, List<String>> musclesByExercise,
            Map<Long, List<EquipmentSummaryResponse>> equipmentByExercise) {
    }
}
