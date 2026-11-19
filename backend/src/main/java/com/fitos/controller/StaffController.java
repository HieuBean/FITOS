package com.fitos.controller;

import com.fitos.dto.user.UserDtos.UserResponse;
import com.fitos.service.UserAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Danh sách nhân sự để Quản lý theo dõi; tài khoản vẫn do Admin quản trị. */
@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER')")
public class StaffController {

    private final UserAdminService userAdminService;

    @GetMapping("/receptionists")
    public ResponseEntity<List<UserResponse>> receptionists() {
        return ResponseEntity.ok(userAdminService.list(null, "ROLE_RECEPTIONIST", null));
    }
}
