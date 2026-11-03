package com.jaxon.smart_gym.profile.internal.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jaxon.smart_gym.common.exception.BusinessException;
import com.jaxon.smart_gym.profile.internal.dto.BodyMetricCommand;
import com.jaxon.smart_gym.profile.internal.dto.HealthConstraintCommand;
import com.jaxon.smart_gym.profile.internal.dto.MemberProfileCommand;
import com.jaxon.smart_gym.profile.internal.dto.MemberProfileView;
import com.jaxon.smart_gym.profile.internal.dto.NutritionProfileCommand;
import com.jaxon.smart_gym.profile.internal.dto.NutritionProfileView;
import com.jaxon.smart_gym.profile.internal.entity.Allergen;
import com.jaxon.smart_gym.profile.internal.entity.BodyMetric;
import com.jaxon.smart_gym.profile.internal.entity.DietaryRestriction;
import com.jaxon.smart_gym.profile.internal.entity.FoodAllergy;
import com.jaxon.smart_gym.profile.internal.entity.FoodPreference;
import com.jaxon.smart_gym.profile.internal.entity.HealthConstraint;
import com.jaxon.smart_gym.profile.internal.entity.MemberProfile;
import com.jaxon.smart_gym.profile.internal.entity.NutritionProfile;
import com.jaxon.smart_gym.profile.internal.entity.TrainingGoal;
import com.jaxon.smart_gym.profile.internal.mapper.AllergenMapper;
import com.jaxon.smart_gym.profile.internal.mapper.BodyMetricMapper;
import com.jaxon.smart_gym.profile.internal.mapper.DietaryRestrictionMapper;
import com.jaxon.smart_gym.profile.internal.mapper.FoodAllergyMapper;
import com.jaxon.smart_gym.profile.internal.mapper.FoodPreferenceMapper;
import com.jaxon.smart_gym.profile.internal.mapper.HealthConstraintMapper;
import com.jaxon.smart_gym.profile.internal.mapper.MemberProfileMapper;
import com.jaxon.smart_gym.profile.internal.mapper.NutritionProfileMapper;
import com.jaxon.smart_gym.profile.internal.mapper.TrainingGoalMapper;

@Service
public class HealthProfileService {

    private static final String ACTIVE = "ACTIVE";
    private static final String INACTIVE = "INACTIVE";
    private static final String DISLIKED_FOOD = "DISLIKED_FOOD";
    private static final String PREFERRED_CUISINE = "PREFERRED_CUISINE";
    private static final String USER_EXPLICIT = "USER_EXPLICIT";

    private final MemberProfileMapper memberProfileMapper;
    private final TrainingGoalMapper trainingGoalMapper;
    private final BodyMetricMapper bodyMetricMapper;
    private final HealthConstraintMapper healthConstraintMapper;
    private final NutritionProfileMapper nutritionProfileMapper;
    private final AllergenMapper allergenMapper;
    private final FoodAllergyMapper foodAllergyMapper;
    private final DietaryRestrictionMapper dietaryRestrictionMapper;
    private final FoodPreferenceMapper foodPreferenceMapper;

    public HealthProfileService(
            MemberProfileMapper memberProfileMapper,
            TrainingGoalMapper trainingGoalMapper,
            BodyMetricMapper bodyMetricMapper,
            HealthConstraintMapper healthConstraintMapper,
            NutritionProfileMapper nutritionProfileMapper,
            AllergenMapper allergenMapper,
            FoodAllergyMapper foodAllergyMapper,
            DietaryRestrictionMapper dietaryRestrictionMapper,
            FoodPreferenceMapper foodPreferenceMapper) {
        this.memberProfileMapper = memberProfileMapper;
        this.trainingGoalMapper = trainingGoalMapper;
        this.bodyMetricMapper = bodyMetricMapper;
        this.healthConstraintMapper = healthConstraintMapper;
        this.nutritionProfileMapper = nutritionProfileMapper;
        this.allergenMapper = allergenMapper;
        this.foodAllergyMapper = foodAllergyMapper;
        this.dietaryRestrictionMapper = dietaryRestrictionMapper;
        this.foodPreferenceMapper = foodPreferenceMapper;
    }

    @Transactional(readOnly = true)
    public MemberProfileView getProfile(Long userId) {
        MemberProfile profile = memberProfileMapper.selectOne(
                new LambdaQueryWrapper<MemberProfile>()
                        .eq(MemberProfile::getUserId, userId));
        if (profile == null) {
            return null;
        }

        List<String> goals = trainingGoalMapper.selectList(
                new LambdaQueryWrapper<TrainingGoal>()
                        .eq(TrainingGoal::getUserId, userId)
                        .eq(TrainingGoal::getStatus, ACTIVE)
                        .orderByAsc(TrainingGoal::getPriority)
                        .orderByAsc(TrainingGoal::getId))
                .stream()
                .map(TrainingGoal::getGoalType)
                .toList();

        return new MemberProfileView(
                profile.getUserId(),
                profile.getGender(),
                profile.getBirthDate(),
                profile.getHeightCm(),
                profile.getWeightKg(),
                profile.getTrainingExperience(),
                profile.getWeeklyTrainingDays(),
                profile.getMaxSessionMinutes(),
                goals,
                profile.getMedicalNotice(),
                profile.getNote(),
                toUtcOffset(profile.getUpdatedAt()));
    }

    @Transactional
    public MemberProfileView saveProfile(Long userId, MemberProfileCommand command) {
        MemberProfile profile = memberProfileMapper.selectOne(
                new LambdaQueryWrapper<MemberProfile>()
                        .eq(MemberProfile::getUserId, userId));
        if (profile == null) {
            profile = new MemberProfile();
            profile.setUserId(userId);
        }

        profile.setGender(command.gender());
        profile.setBirthDate(command.birthDate());
        profile.setHeightCm(command.heightCm());
        profile.setWeightKg(command.weightKg());
        profile.setTrainingExperience(command.trainingExperience());
        profile.setWeeklyTrainingDays(command.weeklyTrainingDays());
        profile.setMaxSessionMinutes(command.maxSessionMinutes());
        profile.setMedicalNotice(command.medicalNotice());
        profile.setNote(command.note());

        if (profile.getId() == null) {
            memberProfileMapper.insert(profile);
        } else {
            memberProfileMapper.update(
                    null,
                    new LambdaUpdateWrapper<MemberProfile>()
                            .eq(MemberProfile::getUserId, userId)
                            .set(MemberProfile::getGender, profile.getGender())
                            .set(MemberProfile::getBirthDate, profile.getBirthDate())
                            .set(MemberProfile::getHeightCm, profile.getHeightCm())
                            .set(MemberProfile::getWeightKg, profile.getWeightKg())
                            .set(MemberProfile::getTrainingExperience, profile.getTrainingExperience())
                            .set(MemberProfile::getWeeklyTrainingDays, profile.getWeeklyTrainingDays())
                            .set(MemberProfile::getMaxSessionMinutes, profile.getMaxSessionMinutes())
                            .set(MemberProfile::getMedicalNotice, profile.getMedicalNotice())
                            .set(MemberProfile::getNote, profile.getNote()));
        }

        replaceGoals(userId, command.goals());
        return getProfile(userId);
    }

    @Transactional
    public BodyMetric addBodyMetric(Long userId, BodyMetricCommand command) {
        BodyMetric metric = new BodyMetric();
        metric.setUserId(userId);
        metric.setMeasuredAt(command.measuredAt()
                .withOffsetSameInstant(ZoneOffset.UTC)
                .toLocalDateTime());
        metric.setWeightKg(command.weightKg());
        metric.setBodyFatPercent(command.bodyFatPercent());
        metric.setWaistCm(command.waistCm());
        metric.setNote(command.note());
        bodyMetricMapper.insert(metric);
        return metric;
    }

    @Transactional(readOnly = true)
    public Page<BodyMetric> listBodyMetrics(
            Long userId,
            java.time.LocalDateTime from,
            java.time.LocalDateTime to,
            long page,
            long pageSize) {
        Page<BodyMetric> resultPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<BodyMetric> query = new LambdaQueryWrapper<BodyMetric>()
                .eq(BodyMetric::getUserId, userId)
                .orderByDesc(BodyMetric::getMeasuredAt)
                .orderByDesc(BodyMetric::getId);
        if (from != null) {
            query.ge(BodyMetric::getMeasuredAt, from);
        }
        if (to != null) {
            query.lt(BodyMetric::getMeasuredAt, to);
        }
        return bodyMetricMapper.selectPage(resultPage, query);
    }

    @Transactional(readOnly = true)
    public List<HealthConstraint> listHealthConstraints(Long userId) {
        return healthConstraintMapper.selectList(
                new LambdaQueryWrapper<HealthConstraint>()
                        .eq(HealthConstraint::getUserId, userId)
                        .eq(HealthConstraint::getActive, true)
                        .orderByAsc(HealthConstraint::getId));
    }

    @Transactional
    public List<HealthConstraint> replaceHealthConstraints(
            Long userId,
            List<HealthConstraintCommand> commands,
            String medicalNotice) {
        MemberProfile profile = memberProfileMapper.selectOne(
                new LambdaQueryWrapper<MemberProfile>()
                        .eq(MemberProfile::getUserId, userId));
        if (profile == null) {
            profile = new MemberProfile();
            profile.setUserId(userId);
            profile.setMedicalNotice(medicalNotice);
            memberProfileMapper.insert(profile);
        } else {
            profile.setMedicalNotice(medicalNotice);
            memberProfileMapper.updateById(profile);
        }

        healthConstraintMapper.update(
                null,
                new LambdaUpdateWrapper<HealthConstraint>()
                        .eq(HealthConstraint::getUserId, userId)
                        .eq(HealthConstraint::getActive, true)
                        .set(HealthConstraint::getActive, false));

        List<HealthConstraint> saved = new ArrayList<>();
        for (HealthConstraintCommand command : safeList(commands)) {
            HealthConstraint constraint = new HealthConstraint();
            constraint.setUserId(userId);
            constraint.setConstraintType(command.type());
            constraint.setBodyPart(command.bodyPart());
            constraint.setSeverity(command.severity());
            constraint.setDescription(command.description());
            constraint.setActive(command.active() == null || command.active());
            constraint.setStartedOn(command.startedOn());
            constraint.setEndedOn(command.endedOn());
            healthConstraintMapper.insert(constraint);
            saved.add(constraint);
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public NutritionProfileView getNutritionProfile(Long userId) {
        NutritionProfile profile = nutritionProfileMapper.selectOne(
                new LambdaQueryWrapper<NutritionProfile>()
                        .eq(NutritionProfile::getUserId, userId));
        if (profile == null) {
            return null;
        }

        List<FoodAllergy> allergies = foodAllergyMapper.selectList(
                new LambdaQueryWrapper<FoodAllergy>()
                        .eq(FoodAllergy::getUserId, userId)
                        .eq(FoodAllergy::getActive, true));
        List<String> allergyCodes = allergies.stream()
                .map(item -> allergenMapper.selectById(item.getAllergenId()))
                .filter(Objects::nonNull)
                .map(Allergen::getCode)
                .toList();

        List<String> restrictions = dietaryRestrictionMapper.selectList(
                new LambdaQueryWrapper<DietaryRestriction>()
                        .eq(DietaryRestriction::getUserId, userId)
                        .eq(DietaryRestriction::getActive, true)
                        .orderByAsc(DietaryRestriction::getId))
                .stream()
                .map(DietaryRestriction::getRestrictionCode)
                .toList();

        List<FoodPreference> preferences = foodPreferenceMapper.selectList(
                new LambdaQueryWrapper<FoodPreference>()
                        .eq(FoodPreference::getUserId, userId)
                        .orderByAsc(FoodPreference::getId));
        List<String> dislikedFoods = preferences.stream()
                .filter(item -> DISLIKED_FOOD.equals(item.getPreferenceType()))
                .map(FoodPreference::getPreferenceValue)
                .toList();
        List<String> preferredCuisines = preferences.stream()
                .filter(item -> PREFERRED_CUISINE.equals(item.getPreferenceType()))
                .map(FoodPreference::getPreferenceValue)
                .toList();

        return new NutritionProfileView(
                profile.getUserId(),
                profile.getDietType(),
                allergyCodes,
                restrictions,
                dislikedFoods,
                preferredCuisines,
                profile.getDailyWaterTargetMl(),
                profile.getNote(),
                toUtcOffset(profile.getUpdatedAt()));
    }

    @Transactional
    public NutritionProfileView saveNutritionProfile(
            Long userId,
            NutritionProfileCommand command) {
        NutritionProfile profile = nutritionProfileMapper.selectOne(
                new LambdaQueryWrapper<NutritionProfile>()
                        .eq(NutritionProfile::getUserId, userId));
        if (profile == null) {
            profile = new NutritionProfile();
            profile.setUserId(userId);
        }
        profile.setDietType(command.dietType());
        profile.setDailyWaterTargetMl(command.dailyWaterMlTarget());
        profile.setNote(command.note());
        if (profile.getId() == null) {
            nutritionProfileMapper.insert(profile);
        } else {
            nutritionProfileMapper.updateById(profile);
        }

        replaceAllergies(userId, command.allergies());
        replaceRestrictions(userId, command.restrictions());
        replacePreferences(userId, DISLIKED_FOOD, command.dislikedFoods());
        replacePreferences(userId, PREFERRED_CUISINE, command.preferredCuisines());
        return getNutritionProfile(userId);
    }

    private void replaceGoals(Long userId, List<String> goalValues) {
        trainingGoalMapper.update(
                null,
                new LambdaUpdateWrapper<TrainingGoal>()
                        .eq(TrainingGoal::getUserId, userId)
                        .eq(TrainingGoal::getStatus, ACTIVE)
                        .set(TrainingGoal::getStatus, INACTIVE));

        int priority = 1;
        for (String goal : normalizedCodes(goalValues)) {
            TrainingGoal trainingGoal = new TrainingGoal();
            trainingGoal.setUserId(userId);
            trainingGoal.setGoalType(goal);
            trainingGoal.setPriority(priority++);
            trainingGoal.setStatus(ACTIVE);
            trainingGoalMapper.insert(trainingGoal);
        }
    }

    private void replaceAllergies(Long userId, List<String> values) {
        foodAllergyMapper.delete(
                new LambdaQueryWrapper<FoodAllergy>()
                        .eq(FoodAllergy::getUserId, userId));
        for (String code : normalizedCodes(values)) {
            Allergen allergen = allergenMapper.selectOne(
                    new LambdaQueryWrapper<Allergen>()
                            .eq(Allergen::getCode, code));
            if (allergen == null) {
                throw new BusinessException(
                        "ALLERGEN_NOT_FOUND",
                        "过敏原不存在: " + code,
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }
            FoodAllergy allergy = new FoodAllergy();
            allergy.setUserId(userId);
            allergy.setAllergenId(allergen.getId());
            allergy.setSeverity("MEDIUM");
            allergy.setSource(USER_EXPLICIT);
            allergy.setActive(true);
            foodAllergyMapper.insert(allergy);
        }
    }

    private void replaceRestrictions(Long userId, List<String> values) {
        dietaryRestrictionMapper.delete(
                new LambdaQueryWrapper<DietaryRestriction>()
                        .eq(DietaryRestriction::getUserId, userId));
        for (String code : normalizedCodes(values)) {
            DietaryRestriction restriction = new DietaryRestriction();
            restriction.setUserId(userId);
            restriction.setRestrictionCode(code);
            restriction.setActive(true);
            dietaryRestrictionMapper.insert(restriction);
        }
    }

    private void replacePreferences(Long userId, String type, List<String> values) {
        foodPreferenceMapper.delete(
                new LambdaQueryWrapper<FoodPreference>()
                        .eq(FoodPreference::getUserId, userId)
                        .eq(FoodPreference::getPreferenceType, type));
        for (String value : distinctTrimmed(values)) {
            FoodPreference preference = new FoodPreference();
            preference.setUserId(userId);
            preference.setPreferenceType(type);
            preference.setPreferenceValue(value);
            preference.setSource(USER_EXPLICIT);
            preference.setConfidence(BigDecimal.ONE);
            foodPreferenceMapper.insert(preference);
        }
    }

    private List<String> normalizedCodes(List<String> values) {
        return distinctTrimmed(values).stream()
                .map(value -> value.toUpperCase(Locale.ROOT))
                .toList();
    }

    private List<String> distinctTrimmed(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(new LinkedHashSet<>(values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList()));
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }

    private OffsetDateTime toUtcOffset(java.time.LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }
}
