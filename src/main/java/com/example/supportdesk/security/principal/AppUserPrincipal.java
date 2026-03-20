package com.example.supportdesk.security.principal;

import com.example.supportdesk.common.enums.UserRole;
import com.example.supportdesk.user.entity.AppUser;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter @AllArgsConstructor
public class AppUserPrincipal implements UserDetails {
    private final Long id;
    private final String fullName;
    private final String username;
    private final String password;
    private final UserRole role;
    //
    public static AppUserPrincipal from(AppUser user) {
        return new AppUserPrincipal(
                user.getId(),
                user.getFullName(),
                user.getUsername(),
                user.getPasswordHash(),
                user.getRole()
        );
    }

    //
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
