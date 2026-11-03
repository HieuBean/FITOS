package com.fitos.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/** Task #57: quản trị người dùng của studio (chỉ ADMIN/MANAGER). */
public final class UserDtos {

    private UserDtos() {
    }

    public static final List<String> ASSIGNABLE_ROLES = List.of(
            "ROLE_ADMIN", "ROLE_MANAGER", "ROLE_RECEPTIONIST", "ROLE_TRAINER", "ROLE_MEMBER");

    public record UserResponse(
            Long id,
            String email,
            String fullName,
            String phone,
            Boolean isActive,
            Long studioId,
            List<String> roles,
            Instant createdAt
    ) {
    }

    public record UserCreateRequest(
            @NotBlank @Email(message = "Email không hợp lệ") String email,
            @NotBlank @Size(min = 8, message = "Mật khẩu tối thiểu 8 ký tự") String password,
            @NotBlank @Size(max = 100) String fullName,
            @Size(max = 20) String phone,
            @NotBlank @Pattern(
                    regexp = "ROLE_ADMIN|ROLE_MANAGER|ROLE_RECEPTIONIST|ROLE_TRAINER|ROLE_MEMBER",
                    message = "Vai trò không hợp lệ") String role
    ) {
    }

    public record RoleUpdateRequest(
            @NotBlank @Pattern(
                    regexp = "ROLE_ADMIN|ROLE_MANAGER|ROLE_RECEPTIONIST|ROLE_TRAINER|ROLE_MEMBER",
                    message = "Vai trò không hợp lệ") String role
    ) {
    }

    public record StatusUpdateRequest(Boolean isActive) {
    }
}
