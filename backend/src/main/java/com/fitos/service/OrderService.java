package com.fitos.service;

import com.fitos.dto.commerce.OrderDtos.OrderCreateRequest;
import com.fitos.dto.commerce.OrderDtos.OrderItemRequest;
import com.fitos.dto.commerce.OrderDtos.OrderItemResponse;
import com.fitos.dto.commerce.OrderDtos.OrderResponse;
import com.fitos.dto.common.PageResponse;
import com.fitos.exception.BadRequestException;
import com.fitos.exception.ResourceNotFoundException;
import com.fitos.model.Customer;
import com.fitos.model.Order;
import com.fitos.model.OrderItem;
import com.fitos.model.ServicePlan;
import com.fitos.repository.CustomerRepository;
import com.fitos.repository.OrderItemRepository;
import com.fitos.repository.OrderRepository;
import com.fitos.repository.ServicePlanRepository;
import com.fitos.security.StudioContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Task #35: tạo đơn hàng với giá tính hoàn toàn ở server (không tin total từ client).
 * Task #36 & #37: danh sách / chi tiết / huỷ đơn PENDING.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    /** Mã giảm giá hợp lệ — giá trị là tỉ lệ giảm. */
    private static final Map<String, BigDecimal> DISCOUNT_CODES = Map.of(
            "FITOS10", new BigDecimal("0.10"),
            "FITOS20", new BigDecimal("0.20")
    );
    private static final DateTimeFormatter REF_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String REF_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ServicePlanRepository planRepository;
    private final CustomerRepository customerRepository;

    @Transactional
    public OrderResponse create(OrderCreateRequest req) {
        Long studioId = StudioContext.currentStudioId();
        Customer customer = customerRepository.findByIdAndStudioId(req.customerId(), studioId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hội viên: " + req.customerId()));

        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> pendingItems = new ArrayList<>();
        for (OrderItemRequest itemRequest : req.items()) {
            ServicePlan plan = planRepository.findByIdAndStudioId(itemRequest.planId(), studioId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gói: " + itemRequest.planId()));
            if (!Boolean.TRUE.equals(plan.getIsActive())) {
                throw new BadRequestException("Gói đang ngừng bán: " + plan.getName());
            }
            BigDecimal unitPrice = plan.getPrice();
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(itemRequest.quantity()))
                    .setScale(2, RoundingMode.HALF_UP);
            subtotal = subtotal.add(lineTotal);
            pendingItems.add(OrderItem.builder()
                    .studioId(studioId)
                    .planId(plan.getId())
                    .quantity(itemRequest.quantity())
                    .unitPrice(unitPrice)
                    .totalPrice(lineTotal)
                    .build());
        }

        BigDecimal discountRate = resolveDiscountRate(req.discountCode());
        BigDecimal discount = subtotal.multiply(discountRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal tax = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.subtract(discount).add(tax).setScale(2, RoundingMode.HALF_UP);

        Order order = Order.builder()
                .studioId(studioId)
                .customerId(customer.getId())
                .orderReference(nextReference())
                .status("PENDING")
                .subtotalAmount(subtotal)
                .discountAmount(discount)
                .taxAmount(tax)
                .totalAmount(total)
                .discountCode(req.discountCode() == null ? null : req.discountCode().trim().toUpperCase())
                .notes(req.notes())
                .build();
        order = orderRepository.save(order);

        final Long orderId = order.getId();
        pendingItems.forEach(item -> item.setOrderId(orderId));
        List<OrderItem> savedItems = orderItemRepository.saveAll(pendingItems);

        log.info("Created order {} for customer {} total {}", order.getOrderReference(), customer.getId(), total);
        return toResponse(order, customer.getFullName(), savedItems);
    }

    @Transactional(readOnly = true)
    public OrderResponse get(Long id) {
        Long studioId = StudioContext.currentStudioId();
        Order order = orderRepository.findByIdAndStudioId(id, studioId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + id));
        return toResponse(order, customerName(order.getCustomerId(), studioId),
                orderItemRepository.findByOrderId(order.getId()));
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> list(String status, Pageable pageable) {
        Long studioId = StudioContext.currentStudioId();
        Page<Order> page = (status == null || status.isBlank())
                ? orderRepository.findByStudioIdOrderByCreatedAtDesc(studioId, pageable)
                : orderRepository.findByStudioIdAndStatusOrderByCreatedAtDesc(studioId, status, pageable);
        return PageResponse.of(page.map(order -> toResponse(order, customerName(order.getCustomerId(), studioId),
                orderItemRepository.findByOrderId(order.getId()))));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> mine(Long userId) {
        Long studioId = StudioContext.currentStudioId();
        Customer customer = customerRepository.findByUserIdAndStudioId(userId, studioId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ hội viên"));
        return orderRepository.findByCustomerIdAndStudioIdOrderByCreatedAtDesc(customer.getId(), studioId).stream()
                .map(order -> toResponse(order, customer.getFullName(),
                        orderItemRepository.findByOrderId(order.getId())))
                .toList();
    }

    @Transactional
    public OrderResponse cancel(Long id) {
        Long studioId = StudioContext.currentStudioId();
        Order order = orderRepository.findByIdAndStudioId(id, studioId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + id));
        if (!"PENDING".equals(order.getStatus())) {
            throw new BadRequestException("Chỉ huỷ được đơn ở trạng thái PENDING (hiện tại: " + order.getStatus() + ")");
        }
        order.setStatus("CANCELLED");
        return toResponse(orderRepository.save(order), customerName(order.getCustomerId(), studioId),
                orderItemRepository.findByOrderId(order.getId()));
    }

    /** Dùng nội bộ bởi PaymentService khi thanh toán thành công. */
    @Transactional
    public void markPaid(Order order) {
        order.setStatus("PAID");
        orderRepository.save(order);
    }

    private BigDecimal resolveDiscountRate(String discountCode) {
        if (discountCode == null || discountCode.isBlank()) {
            return BigDecimal.ZERO;
        }
        BigDecimal rate = DISCOUNT_CODES.get(discountCode.trim().toUpperCase());
        if (rate == null) {
            throw new BadRequestException("Mã giảm giá không hợp lệ hoặc đã hết hạn: " + discountCode);
        }
        return rate;
    }

    private String nextReference() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder suffix = new StringBuilder();
            for (int index = 0; index < 6; index++) {
                suffix.append(REF_ALPHABET.charAt(RANDOM.nextInt(REF_ALPHABET.length())));
            }
            String reference = "ORD-" + LocalDate.now().format(REF_DATE) + "-" + suffix;
            if (!orderRepository.existsByOrderReference(reference)) {
                return reference;
            }
        }
        return "ORD-" + LocalDate.now().format(REF_DATE) + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String customerName(Long customerId, Long studioId) {
        return customerRepository.findByIdAndStudioId(customerId, studioId)
                .map(Customer::getFullName)
                .orElse(null);
    }

    private OrderResponse toResponse(Order order, String customerName, List<OrderItem> items) {
        List<OrderItemResponse> itemResponses = items.stream()
                .map(item -> new OrderItemResponse(
                        item.getId(),
                        item.getPlanId(),
                        planRepository.findById(item.getPlanId()).map(ServicePlan::getName).orElse(null),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getTotalPrice()))
                .toList();
        return new OrderResponse(order.getId(), order.getCustomerId(), customerName, order.getOrderReference(),
                order.getStatus(), order.getSubtotalAmount(), order.getDiscountAmount(), order.getTaxAmount(),
                order.getTotalAmount(), order.getDiscountCode(), order.getNotes(), itemResponses,
                order.getCreatedAt(), order.getUpdatedAt());
    }
}
