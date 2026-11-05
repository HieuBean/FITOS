package com.fitos.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class StudioContext {
    private StudioContext() {}

    public static Long currentStudioId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)
                || principal.getStudioId() == null) {
            throw new org.springframework.security.access.AccessDeniedException("Studio context is required");
        }
        return principal.getStudioId();
    }
}
