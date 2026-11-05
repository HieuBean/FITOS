package com.fitos.controller;

import com.fitos.dto.ops.OpsDtos.ExerciseRequest;
import com.fitos.dto.ops.OpsDtos.ExerciseResponse;
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

/** Task #58: thư viện bài tập (30 bài seed sẵn trong V4_1). */
@RestController
@RequestMapping("/api/exercises")
@RequiredArgsConstructor
@Tag(name = "Exercises", description = "Thư viện bài tập")
public class ExerciseController {

    private final OpsService opsService;

    @GetMapping
    @Operation(summary = "Danh sách bài tập, lọc theo nhóm cơ / category")
    public ResponseEntity<List<ExerciseResponse>> list(
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "muscleGroup", required = false) String muscleGroup) {
        String filter = category != null ? category : muscleGroup;
        return ResponseEntity.ok(opsService.listExercises(filter));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TRAINER')")
    @Operation(summary = "Thêm bài tập mới")
    public ResponseEntity<ExerciseResponse> create(@Valid @RequestBody ExerciseRequest req) {
        return new ResponseEntity<>(opsService.createExercise(req), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TRAINER')")
    @Operation(summary = "Cập nhật bài tập")
    public ResponseEntity<ExerciseResponse> update(@PathVariable("id") Long id,
                                                   @Valid @RequestBody ExerciseRequest req) {
        return ResponseEntity.ok(opsService.updateExercise(id, req));
    }
}
