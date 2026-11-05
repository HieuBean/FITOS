package com.fitos.controller;

import com.fitos.dto.commerce.MembershipDtos.MembershipResponse;
import com.fitos.dto.commerce.MembershipDtos.PauseRequest;
import com.fitos.dto.commerce.MembershipDtos.RenewRequest;
import com.fitos.security.UserPrincipal;
import com.fitos.service.MembershipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Task #42 và #43: vòng đời gói hội viên + cảnh báo sắp hết hạn. */
@RestController
@RequestMapping("/api/memberships")
@RequiredArgsConstructor
@Tag(name = "Memberships", description = "Pause / resume / cancel / renew và danh sách sắp hết hạn")
public class MembershipController {

    private final MembershipService membershipService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST')")
    @Operation(summary = "Danh sách gói hội viên (lọc theo status)")
    public ResponseEntity<List<MembershipResponse>> list(
            @RequestParam(value = "status", required = false) String status) {
        return ResponseEntity.ok(membershipService.list(status));
    }

    @GetMapping("/expiring")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST')")
    @Operation(summary = "Các gói sắp hết hạn trong N ngày (mặc định 7)")
    public ResponseEntity<List<MembershipResponse>> expiring(
            @RequestParam(value = "days", defaultValue = "7") int days) {
        return ResponseEntity.ok(membershipService.expiring(days));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('MEMBER', 'ADMIN')")
    @Operation(summary = "Gói của hội viên đang đăng nhập")
    public ResponseEntity<List<MembershipResponse>> mine(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(membershipService.mine(principal.getId()));
    }

    @PatchMapping("/{id}/pause")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Tạm dừng gói hội viên")
    public ResponseEntity<MembershipResponse> pause(@PathVariable("id") Long id) {
        return ResponseEntity.ok(membershipService.pause(id, true));
    }

    @PatchMapping("/{id}/resume")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Tiếp tục gói — cộng bù số ngày đã tạm dừng vào end_date")
    public ResponseEntity<MembershipResponse> resume(@PathVariable("id") Long id) {
        return ResponseEntity.ok(membershipService.pause(id, false));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Huỷ gói hội viên")
    public ResponseEntity<MembershipResponse> cancel(@PathVariable("id") Long id) {
        return ResponseEntity.ok(membershipService.cancel(id));
    }

    @PostMapping("/{id}/renew")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST')")
    @Operation(summary = "Gia hạn gói (mặc định theo duration_days của plan)")
    public ResponseEntity<MembershipResponse> renew(@PathVariable("id") Long id,
                                                    @RequestBody(required = false) RenewRequest req) {
        return ResponseEntity.ok(membershipService.renew(id, req));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Đổi trạng thái theo hành động PAUSE/RESUME")
    public ResponseEntity<MembershipResponse> status(@PathVariable("id") Long id,
                                                     @RequestBody PauseRequest req) {
        return ResponseEntity.ok(membershipService.pause(id, "PAUSE".equalsIgnoreCase(req.action())));
    }
}
