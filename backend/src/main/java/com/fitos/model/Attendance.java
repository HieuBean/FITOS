package com.fitos.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "attendance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "studio_id", nullable = false)
    private Long studioId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "class_session_id")
    private Long classSessionId;

    @Column(name = "checkin_time")
    @Builder.Default
    private LocalDateTime checkinTime = LocalDateTime.now();

    @Column(name = "method", length = 50, nullable = false)
    @Builder.Default
    private String method = "DESK"; // DESK, QR, MANUAL

    @Column(name = "checked_in_by")
    private Long checkedInBy;

    @Column(name = "notes", length = 255)
    private String notes;
}
