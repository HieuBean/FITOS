package com.fitos.order;

import com.fitos.common.dto.PageResponse;
import com.fitos.common.security.CurrentUserService;
import com.fitos.customer.Customer;
import com.fitos.customer.CustomerRepository;
import com.fitos.order.dto.OrderDtos.CreateOrderRequest;
import com.fitos.order.dto.OrderDtos.OrderItemRequest;
import com.fitos.order.dto.OrderDtos.OrderResponse;
import com.fitos.order.dto.OrderDtos.PaymentSummary;
import com.fitos.payment.Payment;
import com.fitos.payment.PaymentRepository;
import com.fitos.plan.ServicePlan;
import com.fitos.plan.ServicePlanRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Task #35, #36, #37. */
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final int MAX_REFERENCE_ATTEMPTS = 5;

    private final OrderRepository orderRepository;
    private final ServicePlanRepository planRepository;
    private final CustomerRepository customerRepository;
    private final PaymentRepository paymentRepository;
    private final OrderReferenceGenerator referenceGenerator;
    private final CurrentUserService currentUser;
    private final Clock clock;

    // ------------------------------------------------------------------ #35

    @Transactional
    public OrderResponse create(CreateOrderRequest req) {
        boolean staff = currentUser.isStaff();
        Customer customer;
        Long createdBy = null;

        if (staff) {
            if (req.customerId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "customerId is required");
            }
            customer = customerRepository.findById(req.customerId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
            createdBy = currentUser.requireUser().getId();
        } else {
            // MEMBER tự mua: luôn là chính mình, không được tự giảm giá
            customer = currentUser.requireCustomer();
            if (req.discountAmount() != null && req.discountAmount().signum() > 0) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Members cannot apply discounts");
            }
        }

        List<OrderPricing.Line> lines = resolveLines(req.items());
        OrderPricing.Result price;
        try {
            price = OrderPricing.calculate(lines, req.discountAmount());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        Order order = new Order();
        order.setOrderReference(uniqueReference());
        order.setCustomerId(customer.getId());
        order.setSubtotalAmount(price.subtotal());
        order.setDiscountAmount(price.discount());
        order.setTotalAmount(price.total());
        order.setStatus(OrderStatus.PENDING);
        order.setNotes(req.notes());
        order.setCreatedBy(createdBy);
        for (OrderPricing.Line line : lines) {
            ServicePlan p = line.plan();
            order.addItem(new OrderItem(p.getId(), p.getName(), p.getPrice(), line.quantity()));
        }

        Order saved = orderRepository.save(order);
        return OrderResponse.from(saved, customer.getFullName(), customer.getEmail(), List.of());
    }

    /** Lấy plan từ DB theo planId; gộp dòng trùng; chặn plan không tồn tại / đã tắt. */
    private List<OrderPricing.Line> resolveLines(List<OrderItemRequest> items) {
        Map<Long, Integer> qtyByPlan = new LinkedHashMap<>();
        for (OrderItemRequest item : items) {
            qtyByPlan.merge(item.planId(), item.quantityOrDefault(), Integer::sum);
        }
        Map<Long, ServicePlan> plans = planRepository.findAllById(qtyByPlan.keySet()).stream()
                .collect(Collectors.toMap(ServicePlan::getId, Function.identity()));

        List<OrderPricing.Line> lines = new ArrayList<>();
        for (Map.Entry<Long, Integer> e : qtyByPlan.entrySet()) {
            ServicePlan plan = plans.get(e.getKey());
            if (plan == null || !plan.isActive()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Plan " + e.getKey() + " is not available");
            }
            lines.add(new OrderPricing.Line(plan, e.getValue()));
        }
        return lines;
    }

    private String uniqueReference() {
        for (int i = 0; i < MAX_REFERENCE_ATTEMPTS; i++) {
            String ref = referenceGenerator.next();
            if (!orderRepository.existsByOrderReference(ref)) {
                return ref;
            }
        }
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Could not generate order reference");
    }

    // ------------------------------------------------------------------ #36

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> search(OrderStatus status, Long customerId, LocalDate from, LocalDate to,
                                              String q, int page, int size) {
        Specification<Order> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (status != null) {
                ps.add(cb.equal(root.get("status"), status));
            }
            if (customerId != null) {
                ps.add(cb.equal(root.get("customerId"), customerId));
            }
            if (from != null) {
                ps.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startOfDay(from)));
            }
            if (to != null) {
                ps.add(cb.lessThan(root.get("createdAt"), startOfDay(to.plusDays(1))));
            }
            if (q != null && !q.isBlank()) {
                ps.add(cb.like(cb.upper(root.get("orderReference")), "%" + q.trim().toUpperCase() + "%"));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Page<Order> result = orderRepository.findAll(spec,
                PageRequest.of(Math.max(page, 0), clampSize(size), Sort.by(Sort.Direction.DESC, "createdAt")));
        return toPage(result);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> myOrders(int page, int size) {
        Customer me = currentUser.requireCustomer();
        Page<Order> result = orderRepository.findByCustomerIdOrderByCreatedAtDesc(me.getId(),
                PageRequest.of(Math.max(page, 0), clampSize(size)));
        return toPage(result);
    }

    @Transactional(readOnly = true)
    public OrderResponse get(Long id) {
        Order order = orderRepository.findWithItemsById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        assertCanAccess(order);
        Customer c = customerRepository.findById(order.getCustomerId()).orElse(null);
        List<PaymentSummary> payments = paymentRepository.findByOrderIdOrderByCreatedAtDesc(order.getId())
                .stream().map(PaymentSummary::from).toList();
        return OrderResponse.from(order, c == null ? null : c.getFullName(), c == null ? null : c.getEmail(),
                payments);
    }

    // ------------------------------------------------------------------ #37

    @Transactional
    public OrderResponse cancel(Long id) {
        Order order = orderRepository.findWithItemsById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        assertCanAccess(order);
        // Chặn huỷ khi đã có giao dịch thành công (phòng race với webhook)
        if (paymentRepository.existsByOrderIdAndStatus(order.getId(), Payment.Status.SUCCESS)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order already has a successful payment");
        }
        try {
            order.cancel(OffsetDateTime.now(clock));
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
        Customer c = customerRepository.findById(order.getCustomerId()).orElse(null);
        return OrderResponse.from(order, c == null ? null : c.getFullName(), c == null ? null : c.getEmail(),
                List.of());
    }

    // ------------------------------------------------------------------ helpers

    private void assertCanAccess(Order order) {
        if (currentUser.isStaff()) {
            return;
        }
        Customer me = currentUser.requireCustomer();
        if (!Objects.equals(me.getId(), order.getCustomerId())) {
            // 404 thay vì 403 để không lộ sự tồn tại của đơn người khác
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }
    }

    private PageResponse<OrderResponse> toPage(Page<Order> result) {
        Set<Long> customerIds = result.getContent().stream().map(Order::getCustomerId).collect(Collectors.toSet());
        Map<Long, Customer> customers = customerRepository.findAllById(customerIds).stream()
                .collect(Collectors.toMap(Customer::getId, Function.identity()));
        Set<Long> orderIds = result.getContent().stream().map(Order::getId).collect(Collectors.toSet());
        Map<Long, List<PaymentSummary>> payments = orderIds.isEmpty() ? Map.of()
                : paymentRepository.findByOrderIdInOrderByCreatedAtDesc(orderIds).stream()
                .collect(Collectors.groupingBy(Payment::getOrderId,
                        Collectors.mapping(PaymentSummary::from, Collectors.toList())));
        return PageResponse.of(result, o -> {
            Customer c = customers.get(o.getCustomerId());
            return OrderResponse.from(o, c == null ? null : c.getFullName(), c == null ? null : c.getEmail(),
                    payments.getOrDefault(o.getId(), List.of()));
        });
    }

    private OffsetDateTime startOfDay(LocalDate d) {
        return d.atStartOfDay(clock.getZone()).toOffsetDateTime();
    }

    private static int clampSize(int size) {
        return Math.min(Math.max(size, 1), 100);
    }
}
