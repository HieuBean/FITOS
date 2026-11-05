package com.fitos.trainer;

import com.fitos.common.security.Roles;
import com.fitos.trainer.dto.TrainerScheduleDtos.TrainerScheduleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/trainers")
@RequiredArgsConstructor
public class TrainerScheduleController {

    private final TrainerScheduleService scheduleService;

    @GetMapping("/{id}/schedule")
    @PreAuthorize(Roles.STAFF_OR_TRAINER)
    public TrainerScheduleResponse getSchedule(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return scheduleService.getSchedule(id, from, to);
    }
}
