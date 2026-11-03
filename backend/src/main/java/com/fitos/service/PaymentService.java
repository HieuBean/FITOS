package com.fitos.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitos.dto.commerce.PaymentDtos.PaymentCallbackRequest;
import com.fitos.dto.commerce.PaymentDtos.PaymentRequest;
import com.fitos.dto.commerce.PaymentDtos.PaymentResponse;
import com.fitos.exception.BadRequestException;
import com.fitos.exception.ResourceNotFoundException;
import com.fitos.model.Customer;
import com.fitos.model.Order;
import com.fitos.model.Payment;
import com.fitos.repository.CustomerRepository;
import com.fitos.repository.OrderRepository;
import com.fitos.repository.PaymentRepository;
import com.fitos.security.StudioContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Task #38: xử lý thanh toán + trigger ServiceActivation.
 * Task #39: webhook callback idempotent (cùng transactionRef + status chỉ xử lý một lần).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final DateTimeFormatter REF_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String REF_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final OrderService orderService;
    private final ServiceActivationService activationService;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbc;

    @Transactional
    @com.fitos.audit.Auditable(action = "CREATE_PAYMENT", entity = "Payment")
    public PaymentResponse create(PaymentRequest req) {
        Long studioId = StudioContext.currentStudioId();
        Order order = orderRepository.findByIdAndStudioId(req.orderId(), studioId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + req.orderId()));
        if (!"PENDING".equals(order.getStatus())) {
            throw new BadRequestException("Đơn " + order.getOrderReference() + " không ở trạng thái chờ thanh toán");
        }
        if (req.amount() != null && req.amount().compareTo(order.getTotalAmount()) != 0) {
            throw new BadRequestException("Số tiền thanh toán không khớp với đơn hàng ("
                    + order.getTotalAmount().toPlainString() + ")");
        }

        String transactionRef = (req.transactionRef() == null || req.transactionRef().isBlank())
                ? nextReference() : req.transactionRef().trim();
        if (paymentRepository.existsByTransactionRef(transactionRef)) {
            throw new BadRequestException("Mã giao dịch đã tồn tại: " + transactionRef);
        }

        Payment payment = Payment.builder()
                .studioId(studioId)
                .orderId(order.getId())
                .customerId(order.getCustomerId())
                .transactionRef(transactionRef)
                .paymentMethod(req.paymentMethod())
                .amount(order.getTotalAmount())
                .status("PENDING")
                .build();
        payment = paymentRepository.save(payment);

        // Tiền mặt: thu ngay tại quầy nên kích hoạt ngay, không chờ callback.
        if ("CASH".equals(req.paymentMethod())) {
            recordEvent(studioId, payment.getId(), "PAYMENT:" + transactionRef + ":CASH", "SUCCESS", Map.of());
            settle(payment, order);
            return toResponse(payment, false);
        }
        return toResponse(payment, false);
    }

    @Transactional
    @com.fitos.audit.Auditable(action = "PAYMENT_CALLBACK", entity = "Payment")
    public PaymentResponse callback(PaymentCallbackRequest req) {
        Payment payment = paymentRepository.findByTransactionRef(req.transactionRef())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch: " + req.transactionRef()));

        String eventKey = "CALLBACK:" + req.transactionRef() + ":" + req.status();
        Map<String, Object> payload = req.payload() == null ? Map.of() : req.payload();
        boolean firstDelivery = recordEvent(payment.getStudioId(), payment.getId(), eventKey, req.status(), payload);
        if (!firstDelivery) {
            log.info("Callback lặp lại cho {} ({}) — trả kết quả cũ, không kích hoạt lại",
                    req.transactionRef(), req.status());
            return toResponse(payment, true);
        }

        Order order = orderRepository.findById(payment.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn của giao dịch"));

        switch (req.status()) {
            case "SUCCESS" -> {
                if ("SUCCESS".equals(payment.getStatus())) {
                    return toResponse(payment, true);
                }
                payment.setPayloadResponse(writePayload(req.gatewayReference(), payload));
                settle(payment, order);
            }
            case "FAILED" -> {
                if (!"SUCCESS".equals(payment.getStatus())) {
                    payment.setStatus("FAILED");
                    paymentRepository.save(payment);
                }
            }
            case "REFUNDED" -> {
                payment.setStatus("REFUNDED");
                paymentRepository.save(payment);
                order.setStatus("REFUNDED");
                orderRepository.save(order);
                log.info("Hoàn tiền giao dịch {}", payment.getTransactionRef());
            }
            default -> log.debug("Callback PENDING cho {} — không thay đổi trạng thái", req.transactionRef());
        }
        return toResponse(payment, false);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> byOrder(Long orderId) {
        return paymentRepository.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
                .map(payment -> toResponse(payment, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> mine(Long userId) {
        Long studioId = StudioContext.currentStudioId();
        Customer customer = customerRepository.findByUserIdAndStudioId(userId, studioId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ hội viên"));
        return paymentRepository.findByCustomerIdAndStudioIdOrderByCreatedAtDesc(customer.getId(), studioId)
                .stream().map(payment -> toResponse(payment, false)).toList();
    }

    /** Chốt giao dịch: payment SUCCESS → order PAID → kích hoạt quyền lợi. Idempotent. */
    private void settle(Payment payment, Order order) {
        if ("SUCCESS".equals(payment.getStatus())) {
            return;
        }
        payment.setStatus("SUCCESS");
        payment.setPaidAt(Instant.now());
        paymentRepository.save(payment);
        orderService.markPaid(order);
        activationService.activate(order);
        log.info("Thanh toán thành công {} — đơn {}", payment.getTransactionRef(), order.getOrderReference());
    }

    /**
     * Ghi nhận sự kiện thanh toán. `ON CONFLICT DO NOTHING` + UNIQUE(event_key) đảm bảo
     * hai callback đồng thời cũng chỉ có đúng một lần xử lý.
     *
     * @return true nếu đây là lần ghi đầu tiên
     */
    private boolean recordEvent(Long studioId, Long paymentId, String eventKey, String eventType,
                                Map<String, Object> payload) {
        int inserted = jdbc.update(
                "INSERT INTO payment_events (studio_id, payment_id, event_key, event_type, payload) " +
                        "VALUES (?, ?, ?, ?, ?::jsonb) ON CONFLICT (event_key) DO NOTHING",
                studioId, paymentId, eventKey, eventType, writePayload(null, payload));
        return inserted > 0;
    }

    private String nextReference() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder suffix = new StringBuilder();
            for (int index = 0; index < 6; index++) {
                suffix.append(REF_ALPHABET.charAt(RANDOM.nextInt(REF_ALPHABET.length())));
            }
            String reference = "PAY-" + LocalDate.now().format(REF_DATE) + "-" + suffix;
            if (!paymentRepository.existsByTransactionRef(reference)) {
                return reference;
            }
        }
        throw new IllegalStateException("Không sinh được mã giao dịch duy nhất");
    }

    private String writePayload(String gatewayReference, Map<String, Object> payload) {
        try {
            Map<String, Object> body = new java.util.HashMap<>(payload);
            if (gatewayReference != null) {
                body.put("gatewayReference", gatewayReference);
            }
            body.putIfAbsent("receivedAt", Instant.now().toString());
            return objectMapper.writeValueAsString(body);
        } catch (Exception exception) {
            log.warn("Không serialize được payload callback: {}", exception.getMessage());
            return "{}";
        }
    }

    private PaymentResponse toResponse(Payment payment, boolean replay) {
        return new PaymentResponse(payment.getId(), payment.getOrderId(), payment.getCustomerId(),
                payment.getTransactionRef(), payment.getPaymentMethod(), payment.getAmount(), payment.getStatus(),
                payment.getPaidAt(), payment.getCreatedAt(), replay);
    }
}
