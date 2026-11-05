package com.fitos.service;

import com.fitos.dto.PTDto;
import com.fitos.exception.BusinessException;
import com.fitos.exception.ResourceNotFoundException;
import com.fitos.model.PtPackage;
import com.fitos.repository.PtPackageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PTPackageService {

    private final PtPackageRepository ptPackageRepository;

    public List<PTDto.PackageResponse> getCustomerPackages(Long customerId) {
        return ptPackageRepository.findByCustomerId(customerId).stream()
                .map(this::mapToPackageResponse)
                .collect(Collectors.toList());
    }

    /**
     * Task 62: Trừ 1 buổi tập PT với Optimistic Locking (@Version)
     * Ngăn chặn race condition khi hai request trừ số buổi cùng lúc
     */
    @Transactional
    public PTDto.PackageResponse useSession(Long packageId) {
        PtPackage pkg = ptPackageRepository.findById(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("PT Package not found with id: " + packageId));

        if (!"ACTIVE".equalsIgnoreCase(pkg.getStatus())) {
            throw new BusinessException("PT Package is not ACTIVE. Current status: " + pkg.getStatus());
        }

        if (pkg.getRemainingSessions() <= 0) {
            pkg.setStatus("DEPLETED");
            ptPackageRepository.save(pkg);
            throw new BusinessException("No remaining sessions left in this PT Package");
        }

        pkg.setRemainingSessions(pkg.getRemainingSessions() - 1);
        if (pkg.getRemainingSessions() == 0) {
            pkg.setStatus("DEPLETED");
        }

        log.info("Deducted 1 session from PT Package {}. Remaining: {}, version: {}", 
                pkg.getId(), pkg.getRemainingSessions(), pkg.getVersion());

        // Hibernate tự động kiểm tra cột version và tăng version + 1
        PtPackage saved = ptPackageRepository.save(pkg);
        return mapToPackageResponse(saved);
    }

    public PTDto.PackageResponse mapToPackageResponse(PtPackage pkg) {
        return PTDto.PackageResponse.builder()
                .id(pkg.getId())
                .customerId(pkg.getCustomerId())
                .trainerId(null)
                .trainerName(null)
                .totalSessions(pkg.getTotalSessions())
                .remainingSessions(pkg.getRemainingSessions())
                .status(pkg.getStatus())
                .startDate(pkg.getCreatedAt() != null ? pkg.getCreatedAt().toLocalDate() : null)
                .endDate(pkg.getExpiresAt())
                .version(pkg.getVersion())
                .build();
    }
}
