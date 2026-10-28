package com.fitos.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "bookings", uniqueConstraints = {
    @UniqueConstraint(name = "uq_customer_session", columnNames = {"customer_id", "session_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "studio_id", nullable = false)
    private Long studioId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "status", length = 50, nullable = false)
    @Builder.Default
    private String status = "CONFIRMED"; // PENDING, CONFIRMED, CANCELLED, COMPLETED, NO_SHOW

    @Column(name = "booked_at")
    @Builder.Default
    private LocalDateTime bookedAt = LocalDateTime.now();

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;
}
