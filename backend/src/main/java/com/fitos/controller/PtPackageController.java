package com.fitos.controller;

import com.fitos.dto.PTDto;
import com.fitos.service.PTPackageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pt-packages")
@RequiredArgsConstructor
@Tag(name = "PT Package Management API", description = "Quản lý gói tập PT (Task 62)")
public class PtPackageController {

    private final PTPackageService ptPackageService;

    @GetMapping("/me")
    @Operation(summary = "Lấy danh sách gói tập PT của hội viên")
    public ResponseEntity<List<PTDto.PackageResponse>> getMyPackages(@RequestParam(defaultValue = "1") Long customerId) {
        return ResponseEntity.ok(ptPackageService.getCustomerPackages(customerId));
    }

    @PatchMapping("/{id}/use")
    @Operation(summary = "Trừ 1 buổi tập PT với Optimistic Locking (@Version chống race condition)")
    public ResponseEntity<PTDto.PackageResponse> usePackageSession(@PathVariable Long id) {
        return ResponseEntity.ok(ptPackageService.useSession(id));
    }
}
