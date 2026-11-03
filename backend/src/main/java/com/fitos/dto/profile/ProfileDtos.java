package com.fitos.dto.profile;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public final class ProfileDtos {

    private ProfileDtos() {
    }

    public record ProfileResponse(
            Long userId,
            String email,
            String fullName,
            String phone,
            List<String> roles,
            Long studioId,
            CustomerProfile customer
    ) {
    }

    public record CustomerProfile(
            Long id,
            String fullName,
            String phone,
            String email,
            LocalDate dob,
            String gender,
            String fitnessGoal,
            String emergencyContactName,
            String emergencyContactPhone
    ) {
    }

    public record ProfileUpdateRequest(
            @NotBlank(message = "Họ tên không được để trống")
            @Size(max = 100) String fullName,
            @Size(max = 20) String phone,
            LocalDate dob,
            @Size(max = 20) String gender,
            @Size(max = 50) String fitnessGoal,
            @Size(max = 100) String emergencyContactName,
            @Size(max = 20) String emergencyContactPhone
    ) {
    }

    public record PasswordChangeRequest(
            @NotBlank(message = "Mật khẩu hiện tại là bắt buộc") String currentPassword,
            @NotBlank(message = "Mật khẩu mới là bắt buộc")
            @Size(min = 6, max = 100, message = "Mật khẩu mới phải có ít nhất 6 ký tự") String newPassword
    ) {
    }

    public record EmailCheck(@Email String email) {
    }
}
