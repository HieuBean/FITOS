package com.fitos.common.security;

/**
 * Biểu thức @PreAuthorize dùng chung cho các controller của Người B.
 * Giả định role của A: ADMIN, MANAGER, SALES, TRAINER, MEMBER
 * (Spring authority dạng "ROLE_ADMIN"...).
 */
public final class Roles {

    private Roles() {
    }

    public static final String ADMIN = "hasRole('ADMIN')";
    public static final String MANAGEMENT = "hasAnyRole('ADMIN','MANAGER')";
    public static final String STAFF = "hasAnyRole('ADMIN','MANAGER','SALES')";
    public static final String STAFF_OR_TRAINER = "hasAnyRole('ADMIN','MANAGER','SALES','TRAINER')";
    public static final String MEMBER = "hasRole('MEMBER')";
    public static final String STAFF_OR_MEMBER = "hasAnyRole('ADMIN','MANAGER','SALES','MEMBER')";
}
