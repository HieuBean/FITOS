package com.fitos.trainer.dto;

import java.time.OffsetDateTime;
import java.util.List;

public final class TrainerScheduleDtos {

    private TrainerScheduleDtos() {
    }

    public enum SessionType {
        CLASS,
        PT
    }

    public record ScheduleSlot(
            Long id,
            SessionType type,
            String title,
            OffsetDateTime startTime,
            OffsetDateTime endTime,
            String status,
            Integer capacity,
            Integer bookedCount,
            Long customerId,
            String customerName
    ) {
    }

    public record TrainerScheduleResponse(
            Long trainerId,
            String trainerName,
            List<ScheduleSlot> slots
    ) {
    }
}
