package com.fitos.dto.progress;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressResponse {
    private int workoutsThisMonth;
    private int currentStreakDays;
    private int longestStreakDays;
    private BigDecimal currentWeightKg;
    private BigDecimal startingWeightKg;
    private BigDecimal weightChangeKg;
    private List<PersonalRecord> personalRecords;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PersonalRecord {
        private String exerciseName;
        private BigDecimal maxWeightKg;
        private int reps;
        private LocalDate achievedDate;
    }
}
