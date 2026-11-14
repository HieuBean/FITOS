package com.fitos.security;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Xác định studio đang thao tác.
 *
 * <p>{@link StudioContext#currentStudioId()} chỉ dùng cho endpoint yêu cầu đăng nhập.
 * Các endpoint công khai (trang bảng giá, thời khoá biểu trên website) không có phiên
 * đăng nhập nên cần một studio mặc định — nếu không, service sẽ ném
 * {@code AccessDeniedException} và trang công khai nhận 403.
 */
@Component
@RequiredArgsConstructor
public class StudioResolver {

    private final JdbcTemplate jdbc;

    /** Studio của phiên đăng nhập, hoặc null nếu request ẩn danh. */
    public Long sessionStudioIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getStudioId();
        }
        return null;
    }

    /**
     * Studio để đọc dữ liệu công khai: ưu tiên studio của phiên đăng nhập,
     * sau đó tới studio được chỉ định, cuối cùng là studio mặc định (id nhỏ nhất).
     */
    public Long resolveForPublicRead(Long requestedStudioId) {
        Long sessionStudioId = sessionStudioIdOrNull();
        if (sessionStudioId != null) {
            return sessionStudioId;
        }
        if (requestedStudioId != null) {
            return requestedStudioId;
        }
        return defaultStudioId();
    }

    public Long defaultStudioId() {
        List<Long> ids = jdbc.queryForList("SELECT id FROM studios ORDER BY id LIMIT 1", Long.class);
        if (ids.isEmpty()) {
            throw new org.springframework.security.access.AccessDeniedException("Chưa có studio nào trong hệ thống");
        }
        return ids.get(0);
    }
}
