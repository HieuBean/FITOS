package com.fitos.service;

import com.fitos.model.PtPackage;
import com.fitos.model.PtSession;
import com.fitos.repository.PtPackageRepository;
import com.fitos.repository.PtSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PtSessionService {

    private final PtSessionRepository ptSessionRepository;
    private final PtPackageRepository ptPackageRepository;

    /**
     * Task #65 & Task #155:
     * Hoàn thành buổi tập PT có cơ chế Idempotent (Chống trừ trùng buổi tập khi gọi lại cùng completionRef).
     */
    @Transactional
    public PtSession completePtSession(Long sessionId, String completionRef) {
        log.info("Completing PT session: sessionId={}, completionRef={}", sessionId, completionRef);

        // 1. Kiểm tra Idempotency thông qua completionRef duy nhất
        if (completionRef == null || completionRef.isBlank()) {
            throw new IllegalArgumentException("completionRef là bắt buộc");
        }
        if (completionRef != null && !completionRef.isBlank()) {
            var existingByRef = ptSessionRepository.findByCompletionRef(completionRef);
            if (existingByRef.isPresent()) {
                if (!existingByRef.get().getId().equals(sessionId)) {
                    throw new IllegalStateException("completionRef đã thuộc buổi PT khác");
                }
                log.warn("Idempotent hit: Buổi tập đã được hoàn thành trước đó với completionRef={}", completionRef);
                return existingByRef.get();
            }
        }

        // 2. Tìm buổi tập
        PtSession session = ptSessionRepository.findByIdWithLock(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy buổi tập PT ID: " + sessionId));

        // Nếu buổi tập đã hoàn thành với cùng completionRef
        if ("COMPLETED".equals(session.getStatus()) && completionRef != null && completionRef.equals(session.getCompletionRef())) {
            return session;
        }
        if ("COMPLETED".equals(session.getStatus()) || "CANCELLED".equals(session.getStatus())
                || "NO_SHOW".equals(session.getStatus())) {
            throw new IllegalStateException("Không thể hoàn thành buổi PT ở trạng thái " + session.getStatus());
        }

        // 3. Đánh dấu hoàn thành
        session.setStatus("COMPLETED");
        session.setCompletionRef(completionRef);
        session.setUpdatedAt(LocalDateTime.now());
        PtSession savedSession = ptSessionRepository.save(session);

        // 4. Trừ số buổi còn lại trong gói PT Package của hội viên (nếu có liên kết)
        if (session.getPtPackageId() != null) {
            PtPackage pkg = ptPackageRepository.findById(session.getPtPackageId())
                    .orElseThrow(() -> new IllegalStateException("Không tìm thấy gói PT"));
            if (pkg.getRemainingSessions() <= 0) {
                throw new IllegalStateException("Gói PT đã hết buổi");
            }
            pkg.setRemainingSessions(pkg.getRemainingSessions() - 1);
            if (pkg.getRemainingSessions() == 0) pkg.setStatus("DEPLETED");
            ptPackageRepository.save(pkg);
            log.info("Decremented PT package ID {}: remaining={}", pkg.getId(), pkg.getRemainingSessions());
        }

        return savedSession;
    }
}
