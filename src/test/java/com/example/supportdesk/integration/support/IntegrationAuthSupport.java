package com.example.supportdesk.integration.support;

import com.example.supportdesk.security.principal.AppUserPrincipal;
import com.example.supportdesk.security.service.JwtService;
import com.example.supportdesk.user.entity.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IntegrationAuthSupport {
    private final JwtService jwtService;
    //
    public String generateAccessToken(AppUser user) {
        return jwtService.generateToken(AppUserPrincipal.from(user));
    }

    public String bearerToken(AppUser user) {
        return "Bearer " + generateAccessToken(user);
    }

    public String bearerToken(String accessToken) {
        return "Bearer " + accessToken;
    }
}
