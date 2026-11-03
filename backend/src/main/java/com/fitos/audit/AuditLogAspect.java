package com.fitos.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitos.model.AuditLog;
import com.fitos.repository.AuditLogRepository;
import com.fitos.repository.UserRepository;
import com.fitos.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Map;

/**
 * Task #33: AuditLogAspect — @Around @Auditable, ghi lại thao tác quản trị quan trọng.
 * Thiết kế "best effort": lỗi khi ghi audit chỉ log warning, không rollback nghiệp vụ.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Around("@annotation(auditable)")
    public Object around(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        Object result = joinPoint.proceed();
        try {
            persist(auditable, joinPoint, result);
        } catch (Exception exception) {
            log.warn("Không ghi được audit log cho {}: {}", auditable.action(), exception.getMessage());
        }
        return result;
    }

    private void persist(Auditable auditable, ProceedingJoinPoint joinPoint, Object result) {
        Long entityId = extractEntityId(result);
        AuditLog auditLog = AuditLog.builder()
                .studioId(currentStudioId())
                .user(currentUser())
                .action(auditable.action())
                .entityName(auditable.entity())
                .entityId(entityId)
                .details(serializeArguments(joinPoint.getArgs()))
                .ipAddress(currentIp())
                .build();
        auditLogRepository.save(auditLog);
        log.debug("Audit {} {} #{}", auditable.action(), auditable.entity(), entityId);
    }

    /** Audit luôn gắn với studio đang thao tác; nếu không xác định được thì để null. */
    private Long currentStudioId() {
        try {
            return com.fitos.security.StudioContext.currentStudioId();
        } catch (Exception exception) {
            return null;
        }
    }

    private com.fitos.model.User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return userRepository.findById(principal.getId()).orElse(null);
        }
        return null;
    }

    private String currentIp() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return null;
            }
            HttpServletRequest request = attributes.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (Exception exception) {
            return null;
        }
    }

    private Long extractEntityId(Object result) {
        if (result == null) {
            return null;
        }
        try {
            Map<?, ?> map = objectMapper.convertValue(result, Map.class);
            Object id = map.get("id");
            if (id instanceof Number number) {
                return number.longValue();
            }
        } catch (Exception exception) {
            log.debug("Kết quả method không có id để audit: {}", exception.getMessage());
        }
        return null;
    }

    private String serializeArguments(Object[] arguments) {
        try {
            return objectMapper.writeValueAsString(Arrays.stream(arguments)
                    .filter(argument -> argument == null
                            || !(argument instanceof org.springframework.web.multipart.MultipartFile))
                    .toList());
        } catch (Exception exception) {
            return "{\"note\":\"arguments not serializable\"}";
        }
    }
}
