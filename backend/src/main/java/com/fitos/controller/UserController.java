package com.fitos.controller;

import com.fitos.dto.user.UserDtos.RoleUpdateRequest;
import com.fitos.dto.user.UserDtos.StatusUpdateRequest;
import com.fitos.dto.user.UserDtos.UserCreateRequest;
import com.fitos.dto.user.UserDtos.UserResponse;
import com.fitos.security.UserPrincipal;
import com.fitos.service.UserAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Task #57: quản lý người dùng của studio (ADMIN/MANAGER). */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
@Tag(name = "Users", description = "Quản trị tài khoản trong studio")
public class UserController {

    private final UserAdminService userAdminService;

    @GetMapping
    @Operation(summary = "Danh sách người dùng, lọc theo từ khoá/vai trò/trạng thái")
    public ResponseEntity<List<UserResponse>> list(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "role", required = false) String role,
            @RequestParam(value = "activeOnly", required = false) Boolean activeOnly) {
        return ResponseEntity.ok(userAdminService.list(search, role, activeOnly));
    }

    @PostMapping
    @Operation(summary = "Tạo tài khoản nhân viên hoặc hội viên")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest req) {
        return new ResponseEntity<>(userAdminService.create(req), HttpStatus.CREATED);
    }

    @PatchMapping("/{id}/role")
    @Operation(summary = "Đổi vai trò trong studio hiện tại")
    public ResponseEntity<UserResponse> changeRole(@PathVariable("id") Long id,
                                                   @Valid @RequestBody RoleUpdateRequest req,
                                                   @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userAdminService.changeRole(id, req, principal));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Khoá hoặc mở khoá tài khoản")
    public ResponseEntity<UserResponse> setActive(@PathVariable("id") Long id,
                                                  @RequestBody StatusUpdateRequest req,
                                                  @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userAdminService.setActive(id, req, principal));
    }
}
