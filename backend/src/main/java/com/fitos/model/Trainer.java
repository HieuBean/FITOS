package com.fitos.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "trainers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trainer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "studio_id", nullable = false)
    @Builder.Default
    private Long studioId = 1L;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(name = "specialization", length = 150)
    private String specialization;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Builder.Default
    @Column(name = "experience_years")
    private Integer experienceYears = 1;

    @Builder.Default
    @Column(precision = 3, scale = 2)
    private BigDecimal rating = new BigDecimal("5.00");

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder.Default
    @ManyToMany
    @JoinTable(
        name = "trainer_assignments",
        joinColumns = @JoinColumn(name = "trainer_id"),
        inverseJoinColumns = @JoinColumn(name = "customer_id")
    )
    private Set<Customer> assignedCustomers = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    // Backward-compatibility accessors
    public String getName() {
        return fullName;
    }

    public void setName(String name) {
        this.fullName = name;
    }

    public String getSpecialty() {
        return specialization;
    }

    public void setSpecialty(String specialty) {
        this.specialization = specialty;
    }

    public static class TrainerBuilder {
        public TrainerBuilder name(String name) {
            this.fullName = name;
            return this;
        }

        public TrainerBuilder specialty(String specialty) {
            this.specialization = specialty;
            return this;
        }
    }
}
