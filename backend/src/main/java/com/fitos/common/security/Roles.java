package com.fitos.common.security;

/**
 * Biểu thức {@code @PreAuthorize} dùng chung.
 *
 * <p>Vai trò thực tế trong hệ thống (xem {@code V1_1__users.sql} và
 * {@code dto/user/UserDtos.java}): {@code ROLE_ADMIN}, {@code ROLE_MANAGER},
 * {@code ROLE_RECEPTIONIST}, {@code ROLE_TRAINER}, {@code ROLE_MEMBER} và
 * {@code ROLE_PLATFORM_ADMIN}. Trước đây các hằng dưới đây dùng {@code 'SALES'} —
 * vai trò không tồn tại trong DB — khiến lễ tân bị 403 ở đúng nghiệp vụ chính.
 */
public final class Roles {

    private Roles() {
    }

    public static final String PLATFORM_ADMIN = "hasRole('PLATFORM_ADMIN')";
    public static final String ADMIN = "hasRole('ADMIN')";
    public static final String MANAGEMENT = "hasAnyRole('ADMIN','MANAGER')";
    public static final String STAFF = "hasAnyRole('ADMIN','MANAGER','RECEPTIONIST')";
    public static final String STAFF_OR_TRAINER = "hasAnyRole('ADMIN','MANAGER','RECEPTIONIST','TRAINER')";
    public static final String MEMBER = "hasRole('MEMBER')";
    public static final String STAFF_OR_MEMBER = "hasAnyRole('ADMIN','MANAGER','RECEPTIONIST','MEMBER')";
}
