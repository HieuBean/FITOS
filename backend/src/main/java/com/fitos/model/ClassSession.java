package com.fitos.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "class_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "studio_id", nullable = false)
    private Long studioId;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(name = "trainer_id")
    private Long trainerId;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "room", length = 50)
    @Builder.Default
    private String room = "Studio 1";

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @Column(name = "booked_count", nullable = false)
    @Builder.Default
    private Integer bookedCount = 0;

    @Column(name = "status", length = 50, nullable = false)
    @Builder.Default
    private String status = "SCHEDULED";

    public Integer getMaxCapacity() {
        return capacity;
    }

    public void setMaxCapacity(Integer maxCapacity) {
        this.capacity = maxCapacity;
    }

    public Integer getCurrentBookings() {
        return bookedCount;
    }

    public void setCurrentBookings(Integer currentBookings) {
        this.bookedCount = currentBookings;
    }
}
