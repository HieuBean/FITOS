package com.fitos.security;

import com.fitos.model.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final String fullName;
    private final boolean active;
    private final Collection<? extends GrantedAuthority> authorities;
    private final Long studioId;

    public UserPrincipal(Long id, String email, String password, String fullName, boolean active,
                         Collection<? extends GrantedAuthority> authorities) {
        this(id, email, password, fullName, active, authorities, null);
    }

    public UserPrincipal(Long id, String email, String password, String fullName, boolean active,
                         Collection<? extends GrantedAuthority> authorities, Long studioId) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.active = active;
        this.authorities = authorities;
        this.studioId = studioId;
    }

    public static UserPrincipal create(User user) {
        Collection<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toList());

        return new UserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getFullName(),
                Boolean.TRUE.equals(user.getIsActive()),
                authorities,
                user.getStudioId()
        );
    }

    public static UserPrincipal create(User user, Long studioId, List<String> roleNames) {
        return new UserPrincipal(user.getId(), user.getEmail(), user.getPasswordHash(), user.getFullName(),
                Boolean.TRUE.equals(user.getIsActive()),
                roleNames.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList()), studioId);
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
