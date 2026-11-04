package com.fitos.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "exercises")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Exercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "muscle_group", nullable = false)
    private String muscleGroup;

    private String equipment;

    @Builder.Default
    @Column(nullable = false)
    private String difficulty = "BEGINNER";

    @Column(name = "video_url")
    private String videoUrl;

    @Column(name = "image_url")
    private String imageUrl;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    @Builder.Default
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}

