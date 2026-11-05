package com.fitos.controller;

import com.fitos.dto.profile.ProfileDtos.PasswordChangeRequest;
import com.fitos.dto.profile.ProfileDtos.ProfileResponse;
import com.fitos.dto.profile.ProfileDtos.ProfileUpdateRequest;
import com.fitos.security.UserPrincipal;
import com.fitos.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Task #80 (phía API): hồ sơ cá nhân của người đang đăng nhập. */
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "Hồ sơ cá nhân và đổi mật khẩu")
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/me")
    @Operation(summary = "Xem hồ sơ của tôi")
    public ResponseEntity<ProfileResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(profileService.get(principal));
    }

    @PutMapping("/me")
    @Operation(summary = "Cập nhật hồ sơ của tôi")
    public ResponseEntity<ProfileResponse> update(@Valid @RequestBody ProfileUpdateRequest req,
                                                  @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(profileService.update(principal, req));
    }

    @PutMapping("/password")
    @Operation(summary = "Đổi mật khẩu (yêu cầu mật khẩu hiện tại)")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody PasswordChangeRequest req,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        profileService.changePassword(principal, req);
        return ResponseEntity.noContent().build();
    }
}
