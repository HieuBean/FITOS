package com.fitos.controller;

import com.fitos.dto.ClassDto;
import com.fitos.service.ClassSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
@Tag(name = "Classes API", description = "Quản lý danh mục lớp học (Task 60)")
public class ClassController {

    private final ClassSessionService classSessionService;

    @GetMapping
    @Operation(summary = "Lấy danh sách các lớp tập luyện (Yoga, HIIT, v.v.)")
    public ResponseEntity<Page<ClassDto.ClassResponse>> getAll(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(classSessionService.findAllClasses(type, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết một lớp học")
    public ResponseEntity<ClassDto.ClassResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(classSessionService.findClassById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Tạo mới một lớp học")
    public ResponseEntity<ClassDto.ClassResponse> create(@Valid @RequestBody ClassDto.CreateClassRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(classSessionService.createClass(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Cập nhật thông tin lớp học")
    public ResponseEntity<ClassDto.ClassResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ClassDto.UpdateClassRequest req) {
        return ResponseEntity.ok(classSessionService.updateClass(id, req));
    }
}

