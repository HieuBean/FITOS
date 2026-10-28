package com.fitos.auth.password.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class PasswordDtos {

    private PasswordDtos() {
    }

    public record ForgotPasswordRequest(
            @NotBlank @Email String email
    ) {
    }

    public record ResetPasswordRequest(
            @NotBlank String token,
            @NotBlank @Size(min = 6, max = 100) String newPassword
    ) {
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 6, max = 100) String newPassword
    ) {
    }

    public record MessageResponse(String message) {
    }
}
