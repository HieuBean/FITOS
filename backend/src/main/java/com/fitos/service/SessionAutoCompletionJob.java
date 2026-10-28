package com.fitos.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionAutoCompletionJob {

    private final EntityManager entityManager;

    /**
     * Cron Job runs every 15 minutes to automatically mark past class/workout sessions as COMPLETED
     * Per Task #31 of Backend Layer
     */
    @Scheduled(cron = "0 */15 * * * *")
    @Transactional
    public void autoCompletePastSessions() {
        log.info("Running SessionAutoCompletionJob to auto-complete ended sessions...");
        int classUpdated = entityManager.createNativeQuery(
                "UPDATE class_sessions SET status = 'COMPLETED', updated_at = CURRENT_TIMESTAMP " +
                "WHERE status IN ('SCHEDULED', 'IN_PROGRESS') AND " +
                "(session_date < CURRENT_DATE OR (session_date = CURRENT_DATE AND end_time <= CURRENT_TIME))"
        ).executeUpdate();
        int attended = entityManager.createNativeQuery(
                "UPDATE bookings b SET status = 'COMPLETED' FROM class_sessions cs " +
                "WHERE b.session_id = cs.id AND cs.status = 'COMPLETED' AND b.status = 'CONFIRMED' " +
                "AND EXISTS (SELECT 1 FROM attendance a WHERE a.booking_id = b.id)"
        ).executeUpdate();
        int noShows = entityManager.createNativeQuery(
                "UPDATE bookings b SET status = 'NO_SHOW' FROM class_sessions cs " +
                "WHERE b.session_id = cs.id AND cs.status = 'COMPLETED' AND b.status = 'CONFIRMED' " +
                "AND NOT EXISTS (SELECT 1 FROM attendance a WHERE a.booking_id = b.id)"
        ).executeUpdate();
        log.info("Completed {} classes, {} attended bookings and {} no-shows", classUpdated, attended, noShows);
    }
}
