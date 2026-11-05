package com.fitos.controller;

import com.fitos.dto.ops.OpsDtos.TrainerRequest;
import com.fitos.dto.ops.OpsDtos.TrainerResponse;
import com.fitos.service.OpsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Task #45: quản lý huấn luyện viên và gán hội viên. */
@RestController
@RequestMapping("/api/trainers")
@RequiredArgsConstructor
@Tag(name = "Trainers", description = "Huấn luyện viên của studio")
public class TrainerController {

    private final OpsService opsService;

    @GetMapping
    @Operation(summary = "Danh sách huấn luyện viên")
    public ResponseEntity<List<TrainerResponse>> list(
            @RequestParam(value = "activeOnly", defaultValue = "true") boolean activeOnly) {
        return ResponseEntity.ok(opsService.listTrainers(activeOnly));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Thêm huấn luyện viên")
    public ResponseEntity<TrainerResponse> create(@Valid @RequestBody TrainerRequest req) {
        return new ResponseEntity<>(opsService.createTrainer(req), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Cập nhật huấn luyện viên")
    public ResponseEntity<TrainerResponse> update(@PathVariable("id") Long id,
                                                  @Valid @RequestBody TrainerRequest req) {
        return ResponseEntity.ok(opsService.updateTrainer(id, req));
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Gán hội viên cho huấn luyện viên")
    public ResponseEntity<Void> assign(@PathVariable("id") Long id,
                                       @RequestParam("customerId") Long customerId) {
        opsService.assignCustomer(id, customerId);
        return ResponseEntity.noContent().build();
    }
}
