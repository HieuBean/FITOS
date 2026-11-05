package com.fitos.service;

import com.fitos.exception.BadRequestException;
import com.fitos.model.Membership;
import com.fitos.repository.MembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Task #41: bean dùng chung để chặn check-in / đặt chỗ khi hội viên hết hạn gói.
 */
@Service
@RequiredArgsConstructor
public class MembershipValidator {

    private final MembershipRepository membershipRepository;

    /** @throws BadRequestException khi hội viên không có membership ACTIVE còn hạn */
    @Transactional(readOnly = true)
    public Membership requireActive(Long customerId, Long studioId) {
        List<Membership> active = membershipRepository.findActive(customerId, studioId, LocalDate.now());
        if (active.isEmpty()) {
            throw new BadRequestException("Hội viên chưa có gói tập còn hiệu lực — vui lòng gia hạn trước khi điểm danh");
        }
        return active.get(0);
    }

    @Transactional(readOnly = true)
    public String statusOf(Long customerId, Long studioId) {
        if (!membershipRepository.findActive(customerId, studioId, LocalDate.now()).isEmpty()) {
            return "ACTIVE";
        }
        return membershipRepository.findByCustomerIdAndStudioIdOrderByEndDateDesc(customerId, studioId).stream()
                .findFirst()
                .map(Membership::getStatus)
                .orElse("NONE");
    }
}
