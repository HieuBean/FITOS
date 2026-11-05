package com.fitos.service;

import com.fitos.exception.BadRequestException;
import com.fitos.model.User;
import com.fitos.repository.StudioRepository;
import com.fitos.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudioAccessService {
    private final JdbcTemplate jdbc;
    private final StudioRepository studioRepository;

    public Long registrationStudio(Long requestedStudioId) {
        if (requestedStudioId != null) {
            if (!studioRepository.existsById(requestedStudioId)) throw new BadRequestException("Studio not found");
            return requestedStudioId;
        }
        List<Long> ids = jdbc.queryForList("SELECT id FROM studios ORDER BY id LIMIT 2", Long.class);
        if (ids.size() != 1) throw new BadRequestException("Select a studio before registration");
        return ids.get(0);
    }

    public void grantMember(Long userId, Long studioId) {
        jdbc.update("INSERT INTO user_studio_roles (user_id, studio_id, role_id) " +
                "SELECT ?, ?, id FROM roles WHERE name = 'ROLE_MEMBER' ON CONFLICT DO NOTHING", userId, studioId);
    }

    public void grantStaffRole(Long userId, Long studioId, String roleName) {
        if (!List.of("ROLE_MANAGER", "ROLE_RECEPTIONIST", "ROLE_TRAINER").contains(roleName)) {
            throw new BadRequestException("Invalid staff role");
        }
        if (!studioRepository.existsById(studioId)) throw new BadRequestException("Studio not found");
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE id = ? AND is_active = TRUE",
                Long.class, userId);
        if (count == null || count == 0) throw new BadRequestException("Active user not found");
        jdbc.update("INSERT INTO user_studio_roles (user_id, studio_id, role_id) " +
                "SELECT ?, ?, id FROM roles WHERE name = ? ON CONFLICT DO NOTHING", userId, studioId, roleName);
    }

    public List<Long> studioIds(Long userId) {
        Boolean platformAdmin = jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM user_roles ur JOIN roles r ON r.id = ur.role_id " +
                "WHERE ur.user_id = ? AND r.name = 'ROLE_PLATFORM_ADMIN')", Boolean.class, userId);
        if (Boolean.TRUE.equals(platformAdmin)) {
            return jdbc.queryForList("SELECT id FROM studios ORDER BY id", Long.class);
        }
        return jdbc.queryForList("SELECT DISTINCT studio_id FROM user_studio_roles WHERE user_id = ? ORDER BY studio_id", Long.class, userId);
    }

    public UserPrincipal principal(User user, Long requestedStudioId) {
        List<Long> ids = studioIds(user.getId());
        if (ids.isEmpty()) throw new BadRequestException("User has no studio access");
        Long studioId = requestedStudioId;
        if (studioId == null) {
            if (ids.size() != 1) throw new BadRequestException("Select a studio to continue");
            studioId = ids.get(0);
        }
        if (!ids.contains(studioId)) throw new BadRequestException("No access to selected studio");
        List<String> roles = jdbc.queryForList("SELECT r.name FROM user_studio_roles usr JOIN roles r ON r.id = usr.role_id " +
                "WHERE usr.user_id = ? AND usr.studio_id = ?", String.class, user.getId(), studioId);
        boolean platformAdmin = user.getRoles().stream().anyMatch(r -> "ROLE_PLATFORM_ADMIN".equals(r.getName()));
        if (platformAdmin) {
            roles.add("ROLE_PLATFORM_ADMIN");
            if (!roles.contains("ROLE_ADMIN")) roles.add("ROLE_ADMIN");
        }
        return UserPrincipal.create(user, studioId, roles);
    }
}
