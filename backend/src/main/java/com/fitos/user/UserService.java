package com.fitos.user;

import com.fitos.common.dto.PageResponse;
import com.fitos.user.dto.UserDtos.CreateUserRequest;
import com.fitos.user.dto.UserDtos.UpdateUserRoleRequest;
import com.fitos.user.dto.UserDtos.UpdateUserStatusRequest;
import com.fitos.user.dto.UserDtos.UserResponse;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

/**
 * Task #22: Admin User Management.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> listUsers(String search, Role role, Boolean enabled, int page, int size) {
        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                String like = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("fullName")), like),
                        cb.like(cb.lower(root.get("email")), like),
                        cb.like(root.get("phone"), like)
                ));
            }
            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            }
            if (enabled != null) {
                predicates.add(cb.equal(root.get("enabled"), enabled));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<User> result = userRepository.findAll(spec,
                PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100), Sort.by(Sort.Direction.DESC, "createdAt")));

        return PageResponse.of(result, UserResponse::from);
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest req) {
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        }

        User user = new User(
                email,
                passwordEncoder.encode(req.password()),
                req.fullName().trim(),
                req.role()
        );
        user.setPhone(req.phone() == null ? null : req.phone().trim());

        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateStatus(Long id, UpdateUserStatusRequest req) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setEnabled(req.enabled());
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateRole(Long id, UpdateUserRoleRequest req) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setRole(req.role());
        return UserResponse.from(userRepository.save(user));
    }
}
