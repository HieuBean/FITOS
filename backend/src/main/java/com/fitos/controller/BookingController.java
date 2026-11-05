package com.fitos.controller;

import com.fitos.dto.common.PageResponse;
import com.fitos.dto.ops.OpsDtos.BookingRequest;
import com.fitos.dto.ops.OpsDtos.BookingResponse;
import com.fitos.dto.ops.OpsDtos.CancelRequest;
import com.fitos.exception.BadRequestException;
import com.fitos.exception.ResourceNotFoundException;
import com.fitos.model.Booking;
import com.fitos.model.Customer;
import com.fitos.security.UserPrincipal;
import com.fitos.service.BookingOpsService;
import com.fitos.service.BookingService;
import com.fitos.service.CurrentMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Task #49, #50, #51: đặt chỗ với khóa bi quan, danh sách, huỷ và no-show. */
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "Đặt chỗ lớp học")
public class BookingController {

    private final BookingService bookingService;
    private final BookingOpsService bookingOpsService;
    private final CurrentMemberService currentMemberService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Đặt chỗ (hội viên tự đặt, staff đặt hộ qua customerId)")
    public ResponseEntity<BookingResponse> book(@RequestBody BookingRequest req,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        if (req.sessionId() == null) {
            throw new BadRequestException("sessionId là bắt buộc");
        }
        Long customerId;
        if (req.customerId() != null) {
            if (!currentMemberService.isStaff(principal)) {
                throw new BadRequestException("Chỉ nhân viên mới đặt chỗ hộ hội viên");
            }
            customerId = req.customerId();
        } else {
            Customer customer = currentMemberService.require(principal);
            customerId = customer.getId();
        }
        Booking booking = bookingService.bookClass(customerId, req.sessionId());
        return new ResponseEntity<>(findOne(booking.getId()), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST', 'TRAINER')")
    @Operation(summary = "Danh sách đặt chỗ của studio")
    public ResponseEntity<PageResponse<BookingResponse>> list(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(bookingOpsService.list(status, page, size));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('MEMBER', 'ADMIN')")
    @Operation(summary = "Đặt chỗ của tôi")
    public ResponseEntity<List<BookingResponse>> mine(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(bookingOpsService.mine(principal.getId()));
    }

    @GetMapping("/session/{sessionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST', 'TRAINER')")
    @Operation(summary = "Danh sách đặt chỗ của một buổi học")
    public ResponseEntity<List<BookingResponse>> bySession(@PathVariable("sessionId") Long sessionId) {
        return ResponseEntity.ok(bookingOpsService.bySession(sessionId));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Huỷ đặt chỗ (hội viên huỷ của mình, staff huỷ hộ)")
    public ResponseEntity<BookingResponse> cancel(@PathVariable("id") Long id,
                                                  @RequestBody(required = false) CancelRequest req,
                                                  @AuthenticationPrincipal UserPrincipal principal) {
        BookingResponse booking = findOne(id);
        if (!currentMemberService.isStaff(principal)
                && !currentMemberService.ownsBooking(principal, booking.customerId())) {
            throw new BadRequestException("Bạn chỉ có thể huỷ lượt đặt chỗ của chính mình");
        }
        return ResponseEntity.ok(bookingOpsService.cancel(id, req == null ? null : req.reason()));
    }

    @PatchMapping("/{id}/no-show")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST', 'TRAINER')")
    @Operation(summary = "Đánh dấu hội viên không đến")
    public ResponseEntity<BookingResponse> noShow(@PathVariable("id") Long id) {
        return ResponseEntity.ok(bookingOpsService.markNoShow(id));
    }

    private BookingResponse findOne(Long bookingId) {
        return bookingOpsService.get(bookingId);
    }
}
