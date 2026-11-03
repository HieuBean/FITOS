package com.fitos.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitos.dto.commerce.PlanDtos.PlanRequest;
import com.fitos.dto.commerce.PlanDtos.PlanResponse;
import com.fitos.exception.BadRequestException;
import com.fitos.exception.ResourceNotFoundException;
import com.fitos.model.ServicePlan;
import com.fitos.repository.ServicePlanRepository;
import com.fitos.security.StudioContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Task #34: CRUD gói dịch vụ (Membership & PT package) + bật/tắt bán.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanService {

    private final ServicePlanRepository planRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<PlanResponse> list(boolean includeInactive) {
        Long studioId = StudioContext.currentStudioId();
        List<ServicePlan> plans = includeInactive
                ? planRepository.findByStudioIdOrderByPlanTypeAscPriceAsc(studioId)
                : planRepository.findByStudioIdAndIsActiveTrueOrderByPriceAsc(studioId);
        return plans.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PlanResponse get(Long id) {
        ServicePlan plan = planRepository.findByIdAndStudioId(id, StudioContext.currentStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gói dịch vụ: " + id));
        return toResponse(plan);
    }

    @Transactional
    public PlanResponse create(PlanRequest req) {
        Long studioId = StudioContext.currentStudioId();
        if (planRepository.existsByStudioIdAndNameIgnoreCase(studioId, req.name())) {
            throw new BadRequestException("Tên gói đã tồn tại trong studio: " + req.name());
        }
        validateSessionCount(req);
        ServicePlan plan = ServicePlan.builder()
                .studioId(studioId)
                .name(req.name().trim())
                .planType(req.planType())
                .price(req.price())
                .durationDays(req.durationDays())
                .sessionCount(req.sessionCount())
                .description(req.description())
                .features(writeFeatures(req.features()))
                .isActive(req.isActive() == null || req.isActive())
                .build();
        return toResponse(planRepository.save(plan));
    }

    @Transactional
    public PlanResponse update(Long id, PlanRequest req) {
        Long studioId = StudioContext.currentStudioId();
        ServicePlan plan = planRepository.findByIdAndStudioId(id, studioId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gói dịch vụ: " + id));
        if (!plan.getName().equalsIgnoreCase(req.name())
                && planRepository.existsByStudioIdAndNameIgnoreCase(studioId, req.name())) {
            throw new BadRequestException("Tên gói đã tồn tại trong studio: " + req.name());
        }
        validateSessionCount(req);
        plan.setName(req.name().trim());
        plan.setPlanType(req.planType());
        plan.setPrice(req.price());
        plan.setDurationDays(req.durationDays());
        plan.setSessionCount(req.sessionCount());
        plan.setDescription(req.description());
        plan.setFeatures(writeFeatures(req.features()));
        if (req.isActive() != null) {
            plan.setIsActive(req.isActive());
        }
        return toResponse(planRepository.save(plan));
    }

    @Transactional
    public PlanResponse toggleActive(Long id) {
        ServicePlan plan = planRepository.findByIdAndStudioId(id, StudioContext.currentStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gói dịch vụ: " + id));
        plan.setIsActive(!Boolean.TRUE.equals(plan.getIsActive()));
        return toResponse(planRepository.save(plan));
    }

    private void validateSessionCount(PlanRequest req) {
        if ("PT_PACKAGE".equals(req.planType()) && (req.sessionCount() == null || req.sessionCount() <= 0)) {
            throw new BadRequestException("Gói PT_PACKAGE phải có sessionCount > 0");
        }
    }

    private PlanResponse toResponse(ServicePlan plan) {
        return PlanResponse.from(plan, readFeatures(plan.getFeatures()));
    }

    private List<String> readFeatures(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception exception) {
            log.warn("Không parse được features JSON của plan: {}", exception.getMessage());
            return List.of();
        }
    }

    private String writeFeatures(List<String> features) {
        if (features == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(features);
        } catch (Exception exception) {
            throw new BadRequestException("features không hợp lệ");
        }
    }
}
