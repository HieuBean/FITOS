package com.fitos.auth.password;

import com.fitos.auth.password.dto.PasswordDtos.ChangePasswordRequest;
import com.fitos.auth.password.dto.PasswordDtos.ForgotPasswordRequest;
import com.fitos.auth.password.dto.PasswordDtos.MessageResponse;
import com.fitos.auth.password.dto.PasswordDtos.ResetPasswordRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class PasswordController {

    private final PasswordService passwordService;

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest req,
            HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();
        passwordService.forgotPassword(req, clientIp);
        return ResponseEntity.ok(new MessageResponse("If your email is registered, you will receive password reset instructions."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        passwordService.resetPassword(req);
        return ResponseEntity.ok(new MessageResponse("Password has been reset successfully. You can now login."));
    }

    @PutMapping("/change-password")
    public ResponseEntity<MessageResponse> changePassword(@Valid @RequestBody ChangePasswordRequest req) {
        passwordService.changePassword(req);
        return ResponseEntity.ok(new MessageResponse("Password changed successfully."));
    }
}
