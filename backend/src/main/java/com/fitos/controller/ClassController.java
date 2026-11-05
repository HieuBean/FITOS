package com.fitos.controller;

import com.fitos.dto.ops.OpsDtos.ClassRequest;
import com.fitos.dto.ops.OpsDtos.ClassResponse;
import com.fitos.dto.ops.OpsDtos.SessionResponse;
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

/** Task #47: quản lý loại lớp học. GET mở công khai cho trang /classes. */
@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
@Tag(name = "Classes", description = "Loại lớp học của studio")
public class ClassController {

    private final OpsService opsService;

    @GetMapping
    @Operation(summary = "Danh sách lớp học đang hoạt động")
    public ResponseEntity<List<ClassResponse>> list(
            @RequestParam(value = "includeInactive", defaultValue = "false") boolean includeInactive) {
        return ResponseEntity.ok(opsService.listClasses(!includeInactive));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Tạo loại lớp học")
    public ResponseEntity<ClassResponse> create(@Valid @RequestBody ClassRequest req) {
        return new ResponseEntity<>(opsService.createClass(req), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Cập nhật loại lớp học")
    public ResponseEntity<ClassResponse> update(@PathVariable("id") Long id,
                                                @Valid @RequestBody ClassRequest req) {
        return ResponseEntity.ok(opsService.updateClass(id, req));
    }

    @GetMapping("/{id}/sessions")
    @Operation(summary = "Các buổi học của một lớp")
    public ResponseEntity<List<SessionResponse>> sessions(@PathVariable("id") Long id) {
        return ResponseEntity.ok(opsService.sessionsOfClass(id));
    }
}
