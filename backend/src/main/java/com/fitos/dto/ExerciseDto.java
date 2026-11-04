package com.fitos.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

public class ExerciseDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateRequest {
        @NotBlank(message = "Exercise name is required")
        private String name;
        private String description;
        @NotBlank(message = "Muscle group is required")
        private String muscleGroup;
        private String equipment;
        private String difficulty;
        private String videoUrl;
        private String imageUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateRequest {
        private String name;
        private String description;
        private String muscleGroup;
        private String equipment;
        private String difficulty;
        private String videoUrl;
        private String imageUrl;
        private Boolean isActive;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private String name;
        private String description;
        private String muscleGroup;
        private String equipment;
        private String difficulty;
        private String videoUrl;
        private String imageUrl;
        private Boolean isActive;
    }
}

