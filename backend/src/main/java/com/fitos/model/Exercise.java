package com.fitos.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Thư viện bài tập dùng chung cho toàn hệ thống (bảng `exercises` không có cột studio_id —
 * xem migration V4_1 và danh sách bảng tenant trong V4_6).
 */
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

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(name = "muscle_group", nullable = false, length = 100)
    private String muscleGroup;

    @Column(nullable = false, length = 50)
    private String equipment;

    @Builder.Default
    @Column(length = 20)
    private String difficulty = "INTERMEDIATE";

    @Column(columnDefinition = "TEXT")
    private String instruction;

    @Column(name = "video_url", length = 255)
    private String videoUrl;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
