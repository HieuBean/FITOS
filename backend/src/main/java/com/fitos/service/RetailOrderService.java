package com.fitos.service;

import com.fitos.dto.common.PageResponse;
import com.fitos.dto.retail.RetailDtos.*;
import com.fitos.exception.BadRequestException;
import com.fitos.exception.ResourceNotFoundException;
import com.fitos.model.*;
import com.fitos.repository.*;
import com.fitos.retail.RetailPricing;
import com.fitos.security.StudioContext;
import com.fitos.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Bán lẻ tại quầy (POS): tạo đơn nhiều dòng hàng hoá, thanh toán, huỷ, trả hàng.
 *
 * <p>Tất cả thao tác ghi chạy trong một transaction. Sản phẩm được khoá bằng
 * {@code PESSIMISTIC_WRITE} theo thứ tự id tăng dần để tránh deadlock và chống
 * bán vượt tồn khi nhiều thu ngân thao tác cùng lúc.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetailOrderService {

    private static final DateTimeFormatter REF_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String REF_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    /** Mã giảm giá — dùng chung một nguồn với OrderService. */
    private static final Map<String, BigDecimal> DISCOUNT_CODES = Map.of(
            "FITOS10", new BigDecimal("0.10"),
            "FITOS20", new BigDecimal("0.20"));

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CustomerRepository customerRepository;
    private final ServicePlanRepository planRepository;
    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final ClassSessionRepository sessionRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;
    private final UserRepository userRepository;
    private final StudioRepository studioRepository;

    // ===================================================================== create
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public RetailOrderResponse createOrder(RetailOrderCreateRequest req) {
        Long studioId = StudioContext.currentStudioId();
        Long actorId = currentUserId();

        // 1) Idempotency: cùng clientRequestId trả lại đơn cũ, không tạo mới
        if (req.getClientRequestId() != null && !req.getClientRequestId().isBlank()) {
            Optional<Order> existing = orderRepository
                    .findByStudioIdAndClientRequestId(studioId, req.getClientRequestId().trim());
            if (existing.isPresent()) {
                log.info("POS idempotent replay cho clientRequestId={}", req.getClientRequestId());
                RetailOrderResponse resp = toResponse(existing.get());
                resp.setIdempotentReplay(true);
                return resp;
            }
        }

        // 2) Khách hàng phải thuộc studio hiện tại
        Customer customer = customerRepository.findByIdAndStudioId(req.getCustomerId(), studioId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hội viên: " + req.getCustomerId()));

        // 3) Chuẩn hoá & gộp dòng hàng
        List<NormalizedLine> lines = normalizeLines(req.getItems(), studioId);

        // 4) Khoá sản phẩm theo id tăng dần (chống deadlock)
        Map<Long, Product> lockedProducts = new LinkedHashMap<>();
        for (NormalizedLine line : lines) {
            if (line.productId() != null) {
                Product product = productRepository.findByIdWithLock(line.productId(), studioId)
                        .orElseThrow(() -> new BadRequestException("Sản phẩm không tồn tại: " + line.productId()));
                if (!Boolean.TRUE.equals(product.getIsActive())) {
                    throw new BadRequestException("Sản phẩm đã ngừng bán: " + product.getName());
                }
                if (Boolean.TRUE.equals(product.getTrackInventory())
                        && product.getStockQuantity() < line.quantity()) {
                    throw new BadRequestException("Không đủ tồn: " + product.getName()
                            + " (còn " + product.getStockQuantity() + " " + Product.unitLabel(product.getUnit()) + ")");
                }
                lockedProducts.put(product.getId(), product);
            }
        }

        // 5) Dựng dòng hàng + tính tiền hoàn toàn ở server
        List<OrderItem> items = new ArrayList<>();
        List<RetailPricing.Line> pricingLines = new ArrayList<>();
        List<Booking> bookingsToConfirm = new ArrayList<>();

        for (NormalizedLine line : lines) {
            if (line.productId() != null) {
                Product product = lockedProducts.get(line.productId());
                BigDecimal unitPrice = product.getSellPrice();
                BigDecimal unitCost = product.getCostPrice() == null ? BigDecimal.ZERO : product.getCostPrice();
                BigDecimal vatRate = product.getVatRate() == null ? BigDecimal.ZERO : product.getVatRate();
                BigDecimal lineTotal = money(unitPrice.multiply(BigDecimal.valueOf(line.quantity())));

                items.add(OrderItem.builder()
                        .studioId(studioId)
                        .itemType("PRODUCT")
                        .productId(product.getId())
                        .itemName(product.getName())
                        .quantity(line.quantity())
                        .unitPrice(unitPrice)
                        .totalPrice(lineTotal)
                        .unitCost(unitCost)
                        .vatRate(vatRate)
                        .vatAmount(RetailPricing.vatPortion(lineTotal, vatRate))
                        .build());
                pricingLines.add(new RetailPricing.Line(unitPrice, unitCost, line.quantity(), vatRate));
            } else if (line.planId() != null) {
                ServicePlan plan = planRepository.findByIdAndStudioId(line.planId(), studioId)
                        .orElseThrow(() -> new BadRequestException("Gói dịch vụ không tồn tại: " + line.planId()));
                if (!Boolean.TRUE.equals(plan.getIsActive())) {
                    throw new BadRequestException("Gói đang ngừng bán: " + plan.getName());
                }
                BigDecimal unitPrice = plan.getPrice();
                BigDecimal lineTotal = money(unitPrice.multiply(BigDecimal.valueOf(line.quantity())));

                items.add(OrderItem.builder()
                        .studioId(studioId)
                        .itemType("PLAN")
                        .planId(plan.getId())
                        .itemName(plan.getName())
                        .quantity(line.quantity())
                        .unitPrice(unitPrice)
                        .totalPrice(lineTotal)
                        .unitCost(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                        .vatRate(BigDecimal.ZERO)
                        .vatAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                        .build());
                pricingLines.add(new RetailPricing.Line(unitPrice, BigDecimal.ZERO, line.quantity(), BigDecimal.ZERO));
            } else if (line.sessionId() != null) {
                ClassSession session = sessionRepository.findByIdAndStudioId(line.sessionId(), studioId)
                        .orElseThrow(() -> new BadRequestException("Buổi học không tồn tại: " + line.sessionId()));
                if (!"SCHEDULED".equalsIgnoreCase(session.getStatus())) {
                    throw new BadRequestException("Buổi học không còn nhận đặt chỗ: " + session.getId());
                }
                if (session.getBookedCount() >= session.getCapacity()) {
                    throw new BadRequestException("Buổi học đã hết chỗ: " + session.getId());
                }
                // Bán suất lớp: giá bằng 0 (đã nằm trong gói hội viên) — vẫn ghi dòng để
                // giữ chỗ và đối soát. Nếu studio muốn bán lẻ suất lớp thì mở rộng sau.
                Booking booking = bookingRepository.save(Booking.builder()
                        .studioId(studioId)
                        .customerId(customer.getId())
                        .sessionId(session.getId())
                        .status("PENDING")
                        .bookedAt(java.time.LocalDateTime.now())
                        .build());
                session.setBookedCount(session.getBookedCount() + 1);
                sessionRepository.save(session);
                bookingsToConfirm.add(booking);

                items.add(OrderItem.builder()
                        .studioId(studioId)
                        .itemType("BOOKING")
                        .sessionId(session.getId())
                        .bookingId(booking.getId())
                        .itemName("Suất lớp #" + session.getId())
                        .quantity(line.quantity())
                        .unitPrice(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                        .totalPrice(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                        .unitCost(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                        .vatRate(BigDecimal.ZERO)
                        .vatAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                        .build());
            }
        }

        BigDecimal discountRate = req.getDiscountCode() == null ? null
                : DISCOUNT_CODES.get(req.getDiscountCode().trim().toUpperCase());
        RetailPricing.Result priced = RetailPricing.calculate(
                pricingLines, req.getDiscountAmount(), discountRate);

        // 6) Lưu đơn
        Order order = Order.builder()
                .studioId(studioId)
                .customerId(customer.getId())
                .orderReference(nextReference("POS"))
                .orderType("RETAIL")
                .status("PENDING")
                .subtotalAmount(priced.subtotal())
                .discountAmount(priced.discount())
                .taxAmount(priced.tax())
                .totalAmount(priced.total())
                .discountCode(req.getDiscountCode() == null ? null : req.getDiscountCode().trim().toUpperCase())
                .notes(req.getNotes())
                .clientRequestId(req.getClientRequestId() == null || req.getClientRequestId().isBlank()
                        ? null : req.getClientRequestId().trim())
                .createdBy(actorId)
                .build();
        order = orderRepository.save(order);

        final Long orderId = order.getId();
        items.forEach(item -> item.setOrderId(orderId));
        orderItemRepository.saveAll(items);

        // 7) Trừ kho
        for (OrderItem item : items) {
            if ("PRODUCT".equals(item.getItemType())) {
                Product product = lockedProducts.get(item.getProductId());
                inventoryService.deductForSale(studioId, product, item.getQuantity(), orderId, actorId);
            }
        }

        // 8) Thanh toán (nếu có phương thức)
        if (req.getPaymentMethod() != null && !req.getPaymentMethod().isBlank()) {
            settlePayment(order, req.getPaymentMethod(), null, req.getCashReceived(), actorId);
            order = orderRepository.findById(orderId).orElse(order);
            // Xác nhận các booking vừa giữ chỗ
            for (Booking booking : bookingsToConfirm) {
                booking.setStatus("CONFIRMED");
                bookingRepository.save(booking);
            }
        }

        return toResponse(order);
    }

    // ======================================================================= pay
    @Transactional
    public RetailOrderResponse pay(Long orderId, PayOrderRequest req) {
        Long studioId = StudioContext.currentStudioId();
        Long actorId = currentUserId();
        Order order = orderRepository.findByIdAndStudioId(orderId, studioId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn: " + orderId));
        if (!order.isPending()) {
            throw new BadRequestException("Đơn đã ở trạng thái " + order.getStatus() + ", không thu tiền lại");
        }
        settlePayment(order, req.getPaymentMethod(), req.getTransactionRef(), req.getCashReceived(), actorId);

        // Xác nhận booking PENDING của đơn này
        orderItemRepository.findByOrderId(orderId).stream()
                .filter(i -> "BOOKING".equals(i.getItemType()) && i.getBookingId() != null)
                .forEach(i -> bookingRepository.findById(i.getBookingId()).ifPresent(b -> {
                    b.setStatus("CONFIRMED");
                    bookingRepository.save(b);
                }));

        return toResponse(orderRepository.findByIdAndStudioId(orderId, studioId).orElse(order));
    }

    private void settlePayment(Order order, String method, String transactionRef,
                               BigDecimal cashReceived, Long actorId) {
        if ("CASH".equals(method) && cashReceived != null
                && cashReceived.compareTo(order.getTotalAmount()) < 0) {
            throw new BadRequestException("Tiền khách đưa (" + cashReceived.toPlainString()
                    + ") nhỏ hơn tổng tiền (" + order.getTotalAmount().toPlainString() + ")");
        }
        com.fitos.dto.commerce.PaymentDtos.PaymentRequest payReq =
                new com.fitos.dto.commerce.PaymentDtos.PaymentRequest(
                        order.getId(), method, order.getTotalAmount(), transactionRef);
        var payment = paymentService.create(payReq);

        // CASH được PaymentService settle ngay; các phương thức khác cần xác nhận tại quầy
        if (!"CASH".equals(method) && payment != null
                && !"SUCCESS".equalsIgnoreCase(payment.status())) {
            paymentService.callback(new com.fitos.dto.commerce.PaymentDtos.PaymentCallbackRequest(
                    payment.transactionRef(), "SUCCESS", null,
                    Map.of("source", "POS_DESK", "confirmedBy", actorId == null ? "unknown" : actorId)));
        }
    }

    // ==================================================================== cancel
    @Transactional
    public RetailOrderResponse cancel(Long orderId) {
        Long studioId = StudioContext.currentStudioId();
        Long actorId = currentUserId();
        Order order = orderRepository.findByIdAndStudioId(orderId, studioId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn: " + orderId));

        if ("CANCELLED".equals(order.getStatus())) {
            return toResponse(order); // idempotent
        }
        if (!order.isPending()) {
            throw new BadRequestException("Chỉ huỷ được đơn đang chờ thanh toán (hiện tại: "
                    + order.getStatus() + "). Đơn đã thu tiền phải dùng chức năng trả hàng.");
        }

        for (OrderItem item : orderItemRepository.findByOrderId(orderId)) {
            if ("PRODUCT".equals(item.getItemType())) {
                productRepository.findByIdWithLock(item.getProductId(), studioId).ifPresent(p ->
                        inventoryService.restock(studioId, p, item.getQuantity(), orderId, actorId,
                                InventoryTransaction.Type.CANCEL_RETURN, "Huỷ đơn " + order.getOrderReference()));
            } else if ("BOOKING".equals(item.getItemType())) {
                if (item.getBookingId() != null) {
                    bookingRepository.findById(item.getBookingId()).ifPresent(b -> {
                        if (!"CANCELLED".equals(b.getStatus())) {
                            b.setStatus("CANCELLED");
                            bookingRepository.save(b);
                            sessionRepository.findById(b.getSessionId()).ifPresent(s -> {
                                if (s.getBookedCount() > 0) {
                                    s.setBookedCount(s.getBookedCount() - 1);
                                    sessionRepository.save(s);
                                }
                            });
                        }
                    });
                }
            }
        }

        order.setStatus("CANCELLED");
        order.setCancelledAt(Instant.now());
        return toResponse(orderRepository.save(order));
    }

    // ==================================================================== return
    @Transactional
    public RetailOrderResponse createReturn(Long orderId, RetailReturnRequest req) {
        Long studioId = StudioContext.currentStudioId();
        Long actorId = currentUserId();

        Order original = orderRepository.findByIdAndStudioId(orderId, studioId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn: " + orderId));
        if (!"PAID".equals(original.getStatus()) && !"REFUNDED".equals(original.getStatus())) {
            throw new BadRequestException("Chỉ trả hàng cho đơn đã thanh toán (hiện tại: "
                    + original.getStatus() + ")");
        }

        List<OrderItem> originalItems = orderItemRepository.findByOrderId(orderId);
        Map<Long, OrderItem> byId = originalItems.stream()
                .collect(Collectors.toMap(OrderItem::getId, i -> i));

        // Số lượng đã trả trước đó theo từng dòng gốc
        Map<Long, Integer> alreadyReturned = returnedQuantities(studioId, orderId);

        List<OrderItem> returnItems = new ArrayList<>();
        List<RetailPricing.Line> pricingLines = new ArrayList<>();

        for (RetailReturnItemRequest line : req.getItems()) {
            OrderItem source = byId.get(line.getOrderItemId());
            if (source == null) {
                throw new BadRequestException("Dòng hàng không thuộc đơn này: " + line.getOrderItemId());
            }
            if (!"PRODUCT".equals(source.getItemType())) {
                throw new BadRequestException("Chỉ trả được hàng hoá, không trả gói tập hoặc suất lớp");
            }
            int returned = alreadyReturned.getOrDefault(source.getId(), 0);
            if (returned + line.getQuantity() > source.getQuantity()) {
                throw new BadRequestException("Số lượng trả vượt quá số đã mua cho: " + source.getItemName());
            }

            BigDecimal lineTotal = money(source.getUnitPrice().multiply(BigDecimal.valueOf(line.getQuantity())));
            returnItems.add(OrderItem.builder()
                    .studioId(studioId)
                    .itemType("PRODUCT")
                    .productId(source.getProductId())
                    .itemName(source.getItemName())
                    .quantity(line.getQuantity())
                    .unitPrice(source.getUnitPrice())
                    .totalPrice(lineTotal)
                    .unitCost(source.getUnitCost() == null ? BigDecimal.ZERO : source.getUnitCost())
                    .vatRate(source.getVatRate() == null ? BigDecimal.ZERO : source.getVatRate())
                    .vatAmount(RetailPricing.vatPortion(lineTotal, source.getVatRate()))
                    .build());
            pricingLines.add(new RetailPricing.Line(source.getUnitPrice(),
                    source.getUnitCost() == null ? BigDecimal.ZERO : source.getUnitCost(),
                    line.getQuantity(), source.getVatRate()));
        }

        RetailPricing.Result priced = RetailPricing.calculate(pricingLines, null, null);

        Order returnOrder = Order.builder()
                .studioId(studioId)
                .customerId(original.getCustomerId())
                .orderReference(nextReference("RET"))
                .orderType("RETAIL_RETURN")
                .status("REFUNDED")
                .subtotalAmount(priced.subtotal())
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(priced.tax())
                .totalAmount(priced.total())
                .notes("Trả hàng đơn " + original.getOrderReference() + ": " + req.getReason())
                .parentOrderId(original.getId())
                .createdBy(actorId)
                .build();
        returnOrder = orderRepository.save(returnOrder);

        final Long returnOrderId = returnOrder.getId();
        returnItems.forEach(i -> i.setOrderId(returnOrderId));
        orderItemRepository.saveAll(returnItems);

        // Hoàn kho
        for (OrderItem item : returnItems) {
            productRepository.findByIdWithLock(item.getProductId(), studioId).ifPresent(p ->
                    inventoryService.restock(studioId, p, item.getQuantity(), returnOrderId, actorId,
                            InventoryTransaction.Type.RETURN_IN,
                            "Trả hàng từ " + original.getOrderReference()));
        }

        return toResponse(returnOrder);
    }

    private Map<Long, Integer> returnedQuantities(Long studioId, Long originalOrderId) {
        // Các đơn trả hàng có parent_order_id = đơn gốc
        List<Order> returns = orderRepository.findRetailBetween(studioId,
                Instant.EPOCH, Instant.now().plusSeconds(60)).stream()
                .filter(o -> "RETAIL_RETURN".equals(o.getOrderType())
                        && originalOrderId.equals(o.getParentOrderId()))
                .toList();
        Map<Long, Integer> map = new HashMap<>();
        for (Order ret : returns) {
            for (OrderItem item : orderItemRepository.findByOrderId(ret.getId())) {
                Long afterSaleId = findOriginalItemId(originalOrderId, item.getProductId(), item.getUnitPrice());
                if (afterSaleId != null) {
                    map.merge(afterSaleId, item.getQuantity(), Integer::sum);
                }
            }
        }
        return map;
    }

    private Long findOriginalItemId(Long originalOrderId, Long productId, BigDecimal unitPrice) {
        return orderItemRepository.findByOrderId(originalOrderId).stream()
                .filter(i -> Objects.equals(i.getProductId(), productId)
                        && i.getUnitPrice() != null && unitPrice != null
                        && i.getUnitPrice().compareTo(unitPrice) == 0)
                .map(OrderItem::getId)
                .findFirst().orElse(null);
    }

    // ===================================================================== query
    @Transactional(readOnly = true)
    public PageResponse<RetailOrderResponse> list(LocalDate date, String status, int page, int size) {
        Long studioId = StudioContext.currentStudioId();
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize);

        Page<Order> result;
        if (status != null && !status.isBlank()) {
            result = orderRepository.findByStudioIdAndOrderTypeAndStatusOrderByCreatedAtDesc(
                    studioId, "RETAIL", status.toUpperCase(), pageable);
        } else {
            result = orderRepository.findByStudioIdAndOrderTypeOrderByCreatedAtDesc(studioId, "RETAIL", pageable);
        }
        return PageResponse.of(result, this::toResponse);
    }

    @Transactional(readOnly = true)
    public RetailOrderResponse get(Long orderId) {
        Long studioId = StudioContext.currentStudioId();
        return toResponse(orderRepository.findByIdAndStudioId(orderId, studioId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn: " + orderId)));
    }

    @Transactional(readOnly = true)
    public ReceiptResponse receipt(Long orderId) {
        Long studioId = StudioContext.currentStudioId();
        Order order = orderRepository.findByIdAndStudioId(orderId, studioId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn: " + orderId));
        RetailOrderResponse full = toResponse(order);

        Studio studio = studioRepository.findById(studioId).orElse(null);
        String cashierName = order.getCreatedBy() == null ? null
                : userRepository.findById(order.getCreatedBy()).map(u -> u.getFullName()).orElse(null);

        return ReceiptResponse.builder()
                .studioName(studio != null ? studio.getName() : "FITOS")
                .studioAddress(studio != null ? studio.getAddress() : null)
                .studioPhone(studio != null ? studio.getPhone() : null)
                .orderReference(order.getOrderReference())
                .issuedAt(order.getPaidAt() != null ? order.getPaidAt() : order.getCreatedAt())
                .cashierName(cashierName)
                .customerName(full.getCustomerName())
                .customerPhone(full.getCustomerPhone())
                .items(full.getItems())
                .subtotal(full.getSubtotalAmount())
                .discount(full.getDiscountAmount())
                .vat(full.getTaxAmount())
                .total(full.getTotalAmount())
                .paymentMethod(full.getPaymentMethod())
                .cashReceived(full.getCashReceived())
                .changeDue(full.getChangeDue())
                .note(order.getNotes())
                .build();
    }

    // ==================================================================== mapper
    public RetailOrderResponse toResponse(Order order) {
        Long studioId = order.getStudioId();
        Customer customer = customerRepository.findById(order.getCustomerId()).orElse(null);
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());

        Map<Long, Product> products = productRepository
                .findByStudioIdAndIsActiveTrueOrderByNameAsc(studioId).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        List<RetailOrderItemResponse> itemResponses = items.stream()
                .map(i -> {
                    Product p = i.getProductId() == null ? null : products.get(i.getProductId());
                    return RetailOrderItemResponse.builder()
                            .id(i.getId())
                            .itemType(i.getItemType())
                            .productId(i.getProductId())
                            .planId(i.getPlanId())
                            .sessionId(i.getSessionId())
                            .itemName(i.getItemName())
                            .unit(p != null ? p.getUnit() : null)
                            .unitLabel(p != null ? Product.unitLabel(p.getUnit()) : null)
                            .quantity(i.getQuantity())
                            .unitPrice(i.getUnitPrice())
                            .totalPrice(i.getTotalPrice())
                            .vatRate(i.getVatRate())
                            .vatAmount(i.getVatAmount())
                            .build();
                }).toList();

        // Thông tin thanh toán + tiền mặt đã nhận
        var payments = paymentRepository.findByOrderIdOrderByCreatedAtDesc(order.getId());
        String paymentMethod = null;
        String paymentStatus = null;
        BigDecimal cashReceived = null;
        if (!payments.isEmpty()) {
            var latest = payments.get(0);
            paymentMethod = latest.getPaymentMethod();
            paymentStatus = latest.getStatus();
            if ("CASH".equals(paymentMethod)) {
                cashReceived = latest.getAmount();
            }
        }

        return RetailOrderResponse.builder()
                .id(order.getId())
                .orderReference(order.getOrderReference())
                .orderType(order.getOrderType())
                .status(order.getStatus())
                .customerId(order.getCustomerId())
                .customerName(customer != null ? customer.getFullName() : null)
                .customerPhone(customer != null ? customer.getPhone() : null)
                .subtotalAmount(order.getSubtotalAmount())
                .discountAmount(order.getDiscountAmount())
                .taxAmount(order.getTaxAmount())
                .totalAmount(order.getTotalAmount())
                .itemCount(items.stream().mapToInt(i -> i.getQuantity() == null ? 0 : i.getQuantity()).sum())
                .items(itemResponses)
                .paymentMethod(paymentMethod)
                .paymentStatus(paymentStatus)
                .cashReceived(cashReceived)
                .changeDue(cashReceived == null ? null
                        : RetailPricing.changeDue(cashReceived, order.getTotalAmount()))
                .notes(order.getNotes())
                .parentOrderId(order.getParentOrderId())
                .paidAt(order.getPaidAt())
                .createdAt(order.getCreatedAt())
                .build();
    }

    // ==================================================================== helper
    private record NormalizedLine(Long productId, Long planId, Long sessionId, int quantity) {
    }


    /**
     * Gộp dòng trùng sản phẩm (tránh 2 dòng cùng product làm vỡ unique index
     * SALE_OUT), và bắt mỗi dòng chỉ có đúng một loại đích.
     */
    private List<NormalizedLine> normalizeLines(List<RetailOrderItemRequest> items, Long studioId) {
        LinkedHashMap<Long, Integer> productQty = new LinkedHashMap<>();
        List<NormalizedLine> plans = new ArrayList<>();
        List<NormalizedLine> sessions = new ArrayList<>();

        for (RetailOrderItemRequest item : items) {
            int qty = item.getQuantity() == null ? 1 : item.getQuantity();
            int targets = (item.getProductId() != null ? 1 : 0)
                    + (item.getPlanId() != null ? 1 : 0)
                    + (item.getSessionId() != null ? 1 : 0);
            if (targets != 1) {
                throw new BadRequestException("Mỗi dòng hàng phải chọn đúng một loại: sản phẩm, gói hoặc buổi học");
            }
            if (item.getProductId() != null) {
                if (qty > 999) throw new BadRequestException("Số lượng tối đa 999");
                productQty.merge(item.getProductId(), qty, Integer::sum);
            } else if (item.getPlanId() != null) {
                if (qty != 1) throw new BadRequestException("Mỗi gói chỉ bán 1 trong một đơn");
                plans.add(new NormalizedLine(null, item.getPlanId(), null, 1));
            } else {
                if (qty != 1) throw new BadRequestException("Mỗi suất lớp chỉ bán 1 trong một đơn");
                sessions.add(new NormalizedLine(null, null, item.getSessionId(), 1));
            }
        }

        List<NormalizedLine> result = new ArrayList<>();
        productQty.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> result.add(new NormalizedLine(e.getKey(), null, null, e.getValue())));
        result.addAll(plans);
        result.addAll(sessions);
        return result;
    }

    private String nextReference(String prefix) {
        for (int attempt = 0; attempt < 20; attempt++) {
            String candidate = prefix + "-" + LocalDate.now(ZONE).format(REF_DATE) + "-" + randomSuffix(6);
            if (!orderRepository.existsByOrderReference(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Không sinh được mã đơn duy nhất");
    }

    private String randomSuffix(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(REF_ALPHABET.charAt(RANDOM.nextInt(REF_ALPHABET.length())));
        }
        return sb.toString();
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private Long currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getId();
        }
        return null;
    }
}
