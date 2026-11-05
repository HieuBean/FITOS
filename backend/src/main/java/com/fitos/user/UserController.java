package com.fitos.user;

import com.fitos.common.dto.PageResponse;
import com.fitos.common.security.Roles;
import com.fitos.user.dto.UserDtos.CreateUserRequest;
import com.fitos.user.dto.UserDtos.UpdateUserRoleRequest;
import com.fitos.user.dto.UserDtos.UpdateUserStatusRequest;
import com.fitos.user.dto.UserDtos.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Task #22: GET/POST/PATCH /api/users (ADMIN).
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize(Roles.ADMIN)
    public PageResponse<UserResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return userService.listUsers(search, role, enabled, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize(Roles.ADMIN)
    public UserResponse get(@PathVariable Long id) {
        return userService.getUser(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Roles.ADMIN)
    public UserResponse create(@Valid @RequestBody CreateUserRequest req) {
        return userService.createUser(req);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize(Roles.ADMIN)
    public UserResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateUserStatusRequest req) {
        return userService.updateStatus(id, req);
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize(Roles.ADMIN)
    public UserResponse updateRole(@PathVariable Long id, @Valid @RequestBody UpdateUserRoleRequest req) {
        return userService.updateRole(id, req);
    }
}
