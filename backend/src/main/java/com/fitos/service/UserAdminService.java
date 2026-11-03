package com.fitos.service;

import com.fitos.dto.user.UserDtos.RoleUpdateRequest;
import com.fitos.dto.user.UserDtos.StatusUpdateRequest;
import com.fitos.dto.user.UserDtos.UserCreateRequest;
import com.fitos.dto.user.UserDtos.UserResponse;
import com.fitos.exception.BadRequestException;
import com.fitos.exception.ResourceNotFoundException;
import com.fitos.model.Role;
import com.fitos.model.User;
import com.fitos.repository.RoleRepository;
import com.fitos.repository.UserRepository;
import com.fitos.security.StudioContext;
import com.fitos.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Task #57 — quản trị người dùng trong studio.
 * Vai trò là phạm vi theo studio (`user_studio_roles`); tài khoản là danh tính toàn cục.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final StudioAccessService studioAccessService;
    private final JdbcTemplate jdbc;

    private final RowMapper<UserResponse> rowMapper = (rs, rowNum) -> {
        Timestamp createdAt = rs.getTimestamp("created_at");
        return new UserResponse(
                rs.getLong("id"),
                rs.getString("email"),
                rs.getString("full_name"),
                rs.getString("phone"),
                rs.getBoolean("is_active"),
                rs.getLong("studio_id"),
                List.of(rs.getString("roles").split(",")),
                createdAt == null ? null : createdAt.toInstant());
    };

    @Transactional(readOnly = true)
    public List<UserResponse> list(String search, String role, Boolean activeOnly) {
        Long studioId = StudioContext.currentStudioId();
        StringBuilder sql = new StringBuilder("""
                SELECT u.id, u.email, u.full_name, u.phone, u.is_active, u.studio_id, u.created_at,
                       string_agg(DISTINCT r.name, ',') AS roles
                FROM users u
                LEFT JOIN user_studio_roles usr ON usr.user_id = u.id AND usr.studio_id = ?
                LEFT JOIN roles r ON r.id = usr.role_id
                WHERE (u.studio_id = ? OR usr.studio_id IS NOT NULL)
                """);
        List<Object> args = new ArrayList<>(List.of(studioId, studioId));
        if (search != null && !search.isBlank()) {
            sql.append(" AND (LOWER(u.full_name) LIKE ? OR LOWER(u.email) LIKE ? OR u.phone LIKE ?)");
            String like = "%" + search.trim().toLowerCase() + "%";
            args.add(like);
            args.add(like);
            args.add("%" + search.trim() + "%");
        }
        if (activeOnly != null && activeOnly) {
            sql.append(" AND u.is_active = TRUE");
        }
        sql.append(" GROUP BY u.id ORDER BY u.full_name");
        List<UserResponse> users = jdbc.query(sql.toString(), rowMapper, args.toArray());

        if (role != null && !role.isBlank()) {
            return users.stream().filter(user -> user.roles().contains(role)).toList();
        }
        return users;
    }

    @Transactional
    public UserResponse create(UserCreateRequest req) {
        Long studioId = StudioContext.currentStudioId();
        if (userRepository.existsByEmail(req.email().trim().toLowerCase())) {
            throw new BadRequestException("Email đã được sử dụng: " + req.email());
        }
        Role role = roleRepository.findByName(req.role())
                .orElseGet(() -> roleRepository.save(Role.builder().name(req.role()).build()));

        User user = User.builder()
                .studioId(studioId)
                .email(req.email().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(req.password()))
                .fullName(req.fullName().trim())
                .phone(req.phone())
                .isActive(true)
                .roles(new HashSet<>(List.of(role)))
                .build();
        user = userRepository.save(user);

        if ("ROLE_MEMBER".equals(req.role())) {
            studioAccessService.grantMember(user.getId(), studioId);
        } else {
            studioAccessService.grantStaffRole(user.getId(), studioId, req.role());
        }
        log.info("Tạo tài khoản {} ({}) trong studio {}", user.getEmail(), req.role(), studioId);
        return findOne(studioId, user.getId());
    }

    @Transactional
    public UserResponse changeRole(Long userId, RoleUpdateRequest req, UserPrincipal actor) {
        Long studioId = StudioContext.currentStudioId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng: " + userId));
        if (actor != null && actor.getId().equals(userId)) {
            throw new BadRequestException("Không thể tự đổi vai trò của chính mình");
        }
        // Bỏ các vai trò cũ trong studio này rồi cấp vai trò mới
        jdbc.update("DELETE FROM user_studio_roles WHERE user_id = ? AND studio_id = ?", userId, studioId);
        if ("ROLE_MEMBER".equals(req.role())) {
            studioAccessService.grantMember(userId, studioId);
        } else {
            studioAccessService.grantStaffRole(userId, studioId, req.role());
        }
        // Đồng bộ bảng user_roles để CustomUserDetailsService đọc được
        jdbc.update("DELETE FROM user_roles WHERE user_id = ?", userId);
        jdbc.update("INSERT INTO user_roles (user_id, role_id) SELECT ?, id FROM roles WHERE name = ? " +
                "ON CONFLICT DO NOTHING", userId, req.role());
        log.info("Đổi vai trò user {} → {}", userId, req.role());
        return findOne(studioId, user.getId());
    }

    @Transactional
    public UserResponse setActive(Long userId, StatusUpdateRequest req, UserPrincipal actor) {
        Long studioId = StudioContext.currentStudioId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng: " + userId));
        if (actor != null && actor.getId().equals(userId)) {
            throw new BadRequestException("Không thể tự khoá tài khoản của chính mình");
        }
        boolean active = req.isActive() == null || req.isActive();
        user.setIsActive(active);
        userRepository.save(user);
        log.info("{} tài khoản user {}", active ? "Mở khoá" : "Khoá", userId);
        return findOne(studioId, user.getId());
    }

    private UserResponse findOne(Long studioId, Long userId) {
        return jdbc.query("""
                        SELECT u.id, u.email, u.full_name, u.phone, u.is_active, u.studio_id, u.created_at,
                               COALESCE(string_agg(DISTINCT r.name, ','), 'ROLE_MEMBER') AS roles
                        FROM users u
                        LEFT JOIN user_studio_roles usr ON usr.user_id = u.id AND usr.studio_id = ?
                        LEFT JOIN roles r ON r.id = usr.role_id
                        WHERE u.id = ?
                        GROUP BY u.id
                        """, rowMapper, studioId, userId).stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng: " + userId));
    }
}
