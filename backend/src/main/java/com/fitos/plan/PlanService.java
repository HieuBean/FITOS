package com.fitos.plan;

import com.fitos.plan.dto.PlanDtos.PlanRequest;
import com.fitos.plan.dto.PlanDtos.PlanResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

/** Task #34 — CRUD service plans. */
@Service
@RequiredArgsConstructor
public class PlanService {

    private final ServicePlanRepository planRepository;

    @Transactional(readOnly = true)
    public List<PlanResponse> list(boolean includeInactive, PlanType type) {
        List<ServicePlan> plans = includeInactive
                ? planRepository.findAllByOrderByPlanTypeAscPriceAsc()
                : planRepository.findByActiveTrueOrderByPlanTypeAscPriceAsc();
        return plans.stream()
                .filter(p -> type == null || p.getPlanType() == type)
                .map(PlanResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PlanResponse get(Long id, boolean allowInactive) {
        ServicePlan plan = find(id);
        if (!plan.isActive() && !allowInactive) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found");
        }
        return PlanResponse.from(plan);
    }

    @Transactional
    public PlanResponse create(PlanRequest req) {
        validate(req);
        if (planRepository.existsByNameIgnoreCase(req.name().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Plan name already exists");
        }
        ServicePlan plan = new ServicePlan();
        apply(plan, req);
        plan.setActive(req.active() == null || req.active());
        return PlanResponse.from(planRepository.save(plan));
    }

    @Transactional
    public PlanResponse update(Long id, PlanRequest req) {
        validate(req);
        ServicePlan plan = find(id);
        if (planRepository.existsByNameIgnoreCaseAndIdNot(req.name().trim(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Plan name already exists");
        }
        // Đổi loại gói khi đã bán sẽ làm sai logic kích hoạt → chặn
        if (plan.getPlanType() != req.planType()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Plan type cannot be changed; create a new plan instead");
        }
        apply(plan, req);
        if (req.active() != null) {
            plan.setActive(req.active());
        }
        return PlanResponse.from(plan);
    }

    @Transactional
    public PlanResponse setActive(Long id, boolean active) {
        ServicePlan plan = find(id);
        plan.setActive(active);
        return PlanResponse.from(plan);
    }

    ServicePlan find(Long id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found"));
    }

    private void apply(ServicePlan plan, PlanRequest req) {
        plan.setName(req.name().trim());
        plan.setPlanType(req.planType());
        plan.setPrice(req.price());
        plan.setDurationDays(req.durationDays());
        plan.setTotalSessions(req.planType() == PlanType.PT_PACKAGE ? req.totalSessions() : null);
        plan.setDescription(req.description());
        plan.setFeatures(req.features() == null ? new ArrayList<>() : new ArrayList<>(req.features()));
    }

    /** Khớp ck_service_plans_sessions để trả 400 rõ ràng thay vì lỗi DB. */
    private void validate(PlanRequest req) {
        if (req.planType() == PlanType.PT_PACKAGE && req.totalSessions() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PT_PACKAGE requires totalSessions");
        }
        if (req.planType() == PlanType.MEMBERSHIP && req.totalSessions() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MEMBERSHIP must not have totalSessions");
        }
    }
}
