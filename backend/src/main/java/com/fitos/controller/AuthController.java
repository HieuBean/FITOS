package com.fitos.controller;

import com.fitos.dto.auth.AuthResponse;
import com.fitos.dto.auth.LoginRequest;
import com.fitos.dto.auth.RefreshTokenRequest;
import com.fitos.dto.auth.RegisterRequest;
import com.fitos.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user registration, authentication and JWT lifecycle")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register new account")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return new ResponseEntity<>(authService.register(req), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    @Operation(summary = "Log in with email and password")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using valid refresh token")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest req) {
        return ResponseEntity.ok(authService.refreshToken(req));
    }

    @PostMapping("/logout")
    @Operation(summary = "Log out and invalidate token")
    public ResponseEntity<Map<String, String>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody(required = false) Map<String, String> body) {
        authService.logout(authHeader, body == null ? null : body.get("refreshToken"));
        return ResponseEntity.ok(Map.of("message", "Successfully logged out"));
    }
}
