package com.fitos.customer;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Ánh xạ bảng customers (DDL thuộc V1 của Người A, API thuộc Người B — #30–32).
 * Cột giả định: id, user_id, lead_id, full_name, email, phone, date_of_birth,
 * gender, fitness_goal, notes, created_at, updated_at.
 * → Nếu V1 của A khác tên cột, chỉ cần sửa @Column ở đây (xem docs/B_CONTRACTS.md).
 */
@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "lead_id")
    private Long leadId;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(length = 255)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(length = 10)
    private String gender;

    @Column(name = "fitness_goal", length = 50)
    private String fitnessGoal;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
