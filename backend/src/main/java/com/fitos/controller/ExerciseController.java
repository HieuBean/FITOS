package com.fitos.controller;

import com.fitos.dto.ExerciseDto;
import com.fitos.service.ExerciseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/exercises")
@RequiredArgsConstructor
@Tag(name = "Exercises API", description = "Quản lý danh mục bài tập (dùng cho Workout Builder & Exercise Drawer)")
public class ExerciseController {

    private final ExerciseService exerciseService;

    @GetMapping
    @Operation(summary = "Lấy danh sách bài tập (hỗ trợ lọc theo muscleGroup, difficulty, phân trang)")
    public ResponseEntity<Page<ExerciseDto.Response>> getAll(
            @RequestParam(required = false) String muscleGroup,
            @RequestParam(required = false) String difficulty,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(exerciseService.findAll(muscleGroup, difficulty, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết bài tập")
    public ResponseEntity<ExerciseDto.Response> getById(@PathVariable Long id) {
        return ResponseEntity.ok(exerciseService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Tạo mới bài tập")
    public ResponseEntity<ExerciseDto.Response> create(@Valid @RequestBody ExerciseDto.CreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(exerciseService.create(req));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật bài tập")
    public ResponseEntity<ExerciseDto.Response> update(
            @PathVariable Long id,
            @RequestBody ExerciseDto.UpdateRequest req) {
        return ResponseEntity.ok(exerciseService.update(id, req));
    }
}

