package com.example.supportdesk.auth.dto;

import com.example.supportdesk.common.enums.UserRole;
import com.example.supportdesk.security.principal.AppUserPrincipal;

public record MeResponse(
        Long id,
        String fullName,
        String username,
        UserRole role
) {
    public static MeResponse fromPrincipal(AppUserPrincipal principal) {
        return new MeResponse(
                principal.getId(),
                principal.getFullName(),
                principal.getUsername(),
                principal.getRole()
        );
    }
}
