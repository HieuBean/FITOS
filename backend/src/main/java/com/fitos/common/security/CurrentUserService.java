package com.fitos.common.security;

import com.fitos.customer.Customer;
import com.fitos.customer.CustomerRepository;
import com.fitos.user.User;
import com.fitos.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Lấy thông tin người dùng đang đăng nhập.
 * Chỉ dựa vào Spring Security chuẩn: Authentication#getName() = email
 * (JwtAuthFilter của A đặt principal theo email).
 */
@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;

    public Authentication authentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return auth;
    }

    public User requireUser() {
        String email = authentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    /** Customer gắn với user đang đăng nhập (chỉ MEMBER mới có). */
    public Customer requireCustomer() {
        User user = requireUser();
        return customerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Current user has no customer profile"));
    }

    public boolean hasRole(String role) {
        String wanted = "ROLE_" + role;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        for (GrantedAuthority a : auth.getAuthorities()) {
            if (wanted.equals(a.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    /** true nếu là nhân viên (ADMIN / MANAGER / SALES). */
    public boolean isStaff() {
        return hasRole("ADMIN") || hasRole("MANAGER") || hasRole("SALES");
    }
}
