package com.fitos.user.dto;

import com.fitos.user.Role;
import com.fitos.user.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public final class UserDtos {

    private UserDtos() {
    }

    public record CreateUserRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 6, max = 100) String password,
            @NotBlank @Size(max = 100) String fullName,
            String phone,
            @NotNull Role role
    ) {
    }

    public record UpdateUserStatusRequest(
            @NotNull Boolean enabled
    ) {
    }

    public record UpdateUserRoleRequest(
            @NotNull Role role
    ) {
    }

    public record UserResponse(
            Long id,
            String email,
            String fullName,
            String phone,
            Role role,
            boolean enabled,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        public static UserResponse from(User u) {
            return new UserResponse(
                    u.getId(),
                    u.getEmail(),
                    u.getFullName(),
                    u.getPhone(),
                    u.getRole(),
                    u.isEnabled(),
                    u.getCreatedAt(),
                    u.getUpdatedAt()
            );
        }
    }
}
