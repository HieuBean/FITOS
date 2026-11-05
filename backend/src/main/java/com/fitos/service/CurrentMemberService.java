package com.fitos.service;

import com.fitos.exception.ResourceNotFoundException;
import com.fitos.model.Customer;
import com.fitos.repository.CustomerRepository;
import com.fitos.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Tiện ích lấy hồ sơ hội viên của người đang đăng nhập và phân biệt staff/member. */
@Service
@RequiredArgsConstructor
public class CurrentMemberService {

    private static final Set<String> STAFF_ROLES =
            Set.of("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_RECEPTIONIST", "ROLE_TRAINER", "ROLE_PLATFORM_ADMIN");

    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public Customer require(UserPrincipal principal) {
        if (principal == null || principal.getStudioId() == null) {
            throw new ResourceNotFoundException("Không xác định được studio của người dùng");
        }
        return customerRepository.findByUserIdAndStudioId(principal.getId(), principal.getStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản chưa có hồ sơ hội viên trong studio này"));
    }

    public boolean isStaff(UserPrincipal principal) {
        if (principal == null) {
            return false;
        }
        Set<String> authorities = principal.getAuthorities().stream()
                .map(authority -> authority.getAuthority().toUpperCase())
                .collect(Collectors.toSet());
        return authorities.stream().anyMatch(STAFF_ROLES::contains);
    }

    public boolean ownsBooking(UserPrincipal principal, Long customerId) {
        if (principal == null || principal.getStudioId() == null) {
            return false;
        }
        return customerRepository.findByUserIdAndStudioId(principal.getId(), principal.getStudioId())
                .map(customer -> customer.getId().equals(customerId))
                .orElse(false);
    }

    public List<String> roleNames(UserPrincipal principal) {
        return principal == null ? List.of() : principal.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .toList();
    }
}
