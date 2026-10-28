package com.fitos.customer;

import com.fitos.common.dto.PageResponse;
import com.fitos.customer.dto.CustomerDtos.*;
import com.fitos.membership.Membership;
import com.fitos.membership.MembershipRepository;
import com.fitos.membership.PtPackage;
import com.fitos.membership.PtPackageRepository;
import com.fitos.order.Order;
import com.fitos.order.OrderRepository;
import com.fitos.order.OrderStatus;
import com.fitos.order.dto.OrderDtos.OrderResponse;
import com.fitos.order.dto.OrderDtos.PaymentSummary;
import com.fitos.payment.Payment;
import com.fitos.payment.PaymentRepository;
import com.fitos.plan.ServicePlan;
import com.fitos.plan.ServicePlanRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Task #30, #31, #32. */
@Service
@RequiredArgsConstructor
public class CustomerService {

    /** Giá trị đặc biệt cho filter: khách chưa từng có / không còn thẻ ACTIVE|PAUSED. */
    public static final String FILTER_NO_MEMBERSHIP = "NONE";

    private final CustomerRepository customerRepository;
    private final MembershipRepository membershipRepository;
    private final PtPackageRepository ptPackageRepository;
    private final ServicePlanRepository planRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final CustomerActivityQueries activityQueries;
    private final Clock clock;

    // ------------------------------------------------------------------ #30

    @Transactional(readOnly = true)
    public PageResponse<CustomerListItem> list(String search, String membershipStatus, int page, int size) {
        Specification<Customer> spec = searchSpec(search).and(membershipSpec(membershipStatus));
        Page<Customer> result = customerRepository.findAll(spec,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                        Sort.by(Sort.Direction.DESC, "createdAt")));

        List<Long> ids = result.getContent().stream().map(Customer::getId).toList();
        Map<Long, Membership> current = currentMemberships(ids);
        Map<Long, String> planNames = planNames(current.values().stream().map(Membership::getPlanId));
        LocalDate today = LocalDate.now(clock);

        return PageResponse.of(result, c -> {
            Membership m = current.get(c.getId());
            return new CustomerListItem(c.getId(), c.getFullName(), c.getEmail(), c.getPhone(), c.getFitnessGoal(),
                    c.getCreatedAt(), m == null ? null : MembershipBrief.from(m, planNames.get(m.getPlanId()), today));
        });
    }

    private Specification<Customer> searchSpec(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) {
                return cb.conjunction();
            }
            String like = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("fullName")), like),
                    cb.like(cb.lower(root.get("email")), like),
                    cb.like(root.get("phone"), like));
        };
    }

    private Specification<Customer> membershipSpec(String status) {
        return (root, query, cb) -> {
            if (status == null || status.isBlank()) {
                return cb.conjunction();
            }
            Subquery<Long> sub = query.subquery(Long.class);
            var m = sub.from(Membership.class);
            sub.select(m.get("id"));
            if (FILTER_NO_MEMBERSHIP.equalsIgnoreCase(status)) {
                sub.where(cb.equal(m.get("customerId"), root.get("id")),
                        m.get("status").in(Membership.Status.ACTIVE, Membership.Status.PAUSED));
                return cb.not(cb.exists(sub));
            }
            Membership.Status st;
            try {
                st = Membership.Status.valueOf(status.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown membership status: " + status);
            }
            Predicate p = cb.and(cb.equal(m.get("customerId"), root.get("id")), cb.equal(m.get("status"), st));
            sub.where(p);
            return cb.exists(sub);
        };
    }

    // ------------------------------------------------------------------ #31

    /**
     * Không bọc @Transactional: các truy vấn sang module khác có thể lỗi (bảng chưa tồn tại)
     * và trong PostgreSQL một lỗi sẽ làm hỏng cả transaction đang mở.
     */
    public CustomerDetailResponse getDetail(Long id) {
        Customer c = find(id);
        LocalDate today = LocalDate.now(clock);

        List<Membership> memberships = membershipRepository.findByCustomerIdOrderByEndDateDesc(id);
        List<PtPackage> packages = ptPackageRepository.findByCustomerIdOrderByExpiryDateDesc(id);
        List<Order> orders = orderRepository.findTop20ByCustomerIdOrderByCreatedAtDesc(id);

        Map<Long, String> planNames = planNames(Stream.concat(
                memberships.stream().map(Membership::getPlanId),
                packages.stream().map(PtPackage::getPlanId)));

        Map<Long, List<PaymentSummary>> paymentsByOrder = orders.isEmpty() ? Map.of()
                : paymentRepository.findByOrderIdInOrderByCreatedAtDesc(orders.stream().map(Order::getId).toList())
                .stream().collect(Collectors.groupingBy(Payment::getOrderId,
                        Collectors.mapping(PaymentSummary::from, Collectors.toList())));

        List<MembershipBrief> membershipBriefs = memberships.stream()
                .map(m -> MembershipBrief.from(m, planNames.get(m.getPlanId()), today)).toList();
        MembershipBrief currentMembership = pickCurrent(memberships)
                .map(m -> MembershipBrief.from(m, planNames.get(m.getPlanId()), today)).orElse(null);

        List<OrderResponse> orderResponses = orders.stream()
                .map(o -> OrderResponse.from(o, c.getFullName(), c.getEmail(),
                        paymentsByOrder.getOrDefault(o.getId(), List.of())))
                .toList();

        BigDecimal totalSpent = orders.stream().filter(o -> o.getStatus() == OrderStatus.PAID)
                .map(Order::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        long paidOrders = orders.stream().filter(o -> o.getStatus() == OrderStatus.PAID).count();

        CustomerStats stats = new CustomerStats(totalSpent, paidOrders,
                activityQueries.countBookings(id),
                activityQueries.visitsLast30Days(id),
                activityQueries.workoutsLast30Days(id),
                activityQueries.lastVisit(id));

        return new CustomerDetailResponse(
                CustomerProfile.from(c),
                currentMembership,
                membershipBriefs,
                packages.stream().map(p -> PtPackageBrief.from(p, planNames.get(p.getPlanId()))).toList(),
                orderResponses,
                activityQueries.recentBookings(id),
                activityQueries.recentWorkouts(id),
                activityQueries.recentAttendance(id),
                stats);
    }

    // ------------------------------------------------------------------ #32

    @Transactional
    public CustomerProfile update(Long id, UpdateCustomerRequest req) {
        Customer c = find(id);
        String phone = req.phone() == null || req.phone().isBlank() ? null : req.phone().trim();
        if (phone != null && customerRepository.existsByPhoneAndIdNot(phone, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone number already used by another customer");
        }
        c.setFullName(req.fullName().trim());
        c.setPhone(phone);
        c.setDateOfBirth(req.dateOfBirth());
        c.setGender(req.gender());
        c.setFitnessGoal(req.fitnessGoal());
        c.setNotes(req.notes());
        return CustomerProfile.from(customerRepository.save(c));
    }

    // ------------------------------------------------------------------ helpers

    private Customer find(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
    }

    /** Thẻ "hiện tại": ưu tiên ACTIVE → PAUSED → thẻ kết thúc gần nhất. */
    static Optional<Membership> pickCurrent(List<Membership> memberships) {
        Comparator<Membership> byPriority = Comparator
                .comparingInt((Membership m) -> switch (m.getStatus()) {
                    case ACTIVE -> 0;
                    case PAUSED -> 1;
                    case EXPIRED -> 2;
                    case CANCELLED -> 3;
                })
                .thenComparing(Membership::getEndDate, Comparator.reverseOrder());
        return memberships.stream().min(byPriority);
    }

    private Map<Long, Membership> currentMemberships(List<Long> customerIds) {
        if (customerIds.isEmpty()) {
            return Map.of();
        }
        return membershipRepository.findByCustomerIdInOrderByEndDateDesc(customerIds).stream()
                .collect(Collectors.groupingBy(Membership::getCustomerId))
                .entrySet().stream()
                .map(e -> Map.entry(e.getKey(), pickCurrent(e.getValue())))
                .filter(e -> e.getValue().isPresent())
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()));
    }

    private Map<Long, String> planNames(Stream<Long> planIds) {
        Set<Long> ids = planIds.filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return planRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(ServicePlan::getId, ServicePlan::getName, (a, b) -> a));
    }
}
