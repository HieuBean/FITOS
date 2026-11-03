package com.fitos.service;

import com.fitos.model.Membership;
import com.fitos.model.Order;
import com.fitos.model.OrderItem;
import com.fitos.model.PtPackage;
import com.fitos.model.ServicePlan;
import com.fitos.repository.MembershipRepository;
import com.fitos.repository.OrderItemRepository;
import com.fitos.repository.PtPackageRepository;
import com.fitos.repository.ServicePlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Task #40: tách khỏi PaymentService — sau khi đơn được thanh toán, sinh quyền lợi
 * (membership hoặc gói PT) tương ứng với từng hạng mục đã mua.
 * Idempotent theo (order_id, customer_id): callback lặp lại không tạo gói thứ hai.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceActivationService {

    private final OrderItemRepository orderItemRepository;
    private final ServicePlanRepository planRepository;
    private final MembershipRepository membershipRepository;
    private final PtPackageRepository ptPackageRepository;

    @Transactional
    public void activate(Order order) {
        LocalDate today = LocalDate.now();
        for (OrderItem item : orderItemRepository.findByOrderId(order.getId())) {
            ServicePlan plan = planRepository.findById(item.getPlanId()).orElse(null);
            if (plan == null) {
                log.warn("Activation bỏ qua hạng mục {}: không tìm thấy gói {}", item.getId(), item.getPlanId());
                continue;
            }
            int quantity = item.getQuantity() == null ? 1 : item.getQuantity();
            for (int unit = 0; unit < quantity; unit++) {
                if ("MEMBERSHIP".equals(plan.getPlanType())) {
                    activateMembership(order, plan, today, unit);
                } else if ("PT_PACKAGE".equals(plan.getPlanType())) {
                    activatePtPackage(order, plan, today, unit);
                }
            }
        }
    }

    private void activateMembership(Order order, ServicePlan plan, LocalDate today, int unit) {
        if (unit == 0 && membershipRepository.existsByOrderIdAndCustomerIdAndStudioId(
                order.getId(), order.getCustomerId(), order.getStudioId())) {
            log.info("Membership cho order {} đã tồn tại — bỏ qua (idempotent)", order.getOrderReference());
            return;
        }
        Membership membership = Membership.builder()
                .studioId(order.getStudioId())
                .customerId(order.getCustomerId())
                .planId(plan.getId())
                .orderId(order.getId())
                .startDate(today)
                .endDate(today.plusDays(plan.getDurationDays()))
                .status("ACTIVE")
                .autoRenew(false)
                .build();
        membershipRepository.save(membership);
        log.info("Activated MEMBERSHIP {} cho customer {} đến {}", plan.getName(),
                order.getCustomerId(), membership.getEndDate());
    }

    private void activatePtPackage(Order order, ServicePlan plan, LocalDate today, int unit) {
        if (unit == 0 && ptPackageRepository.existsByOrderIdAndCustomerIdAndStudioId(
                order.getId(), order.getCustomerId(), order.getStudioId())) {
            log.info("PT package cho order {} đã tồn tại — bỏ qua (idempotent)", order.getOrderReference());
            return;
        }
        int sessions = plan.getSessionCount() == null ? 0 : plan.getSessionCount();
        if (sessions <= 0) {
            log.warn("Gói PT {} không có sessionCount — bỏ qua kích hoạt", plan.getName());
            return;
        }
        PtPackage ptPackage = PtPackage.builder()
                .studioId(order.getStudioId())
                .customerId(order.getCustomerId())
                .planId(plan.getId())
                .orderId(order.getId())
                .totalSessions(sessions)
                .remainingSessions(sessions)
                .expiresAt(today.plusDays(plan.getDurationDays()))
                .status("ACTIVE")
                .build();
        ptPackageRepository.save(ptPackage);
        log.info("Activated PT_PACKAGE {} ({} buổi) cho customer {}", plan.getName(), sessions, order.getCustomerId());
    }
}
