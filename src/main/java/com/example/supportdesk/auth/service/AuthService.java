package com.example.supportdesk.auth.service;

import com.example.supportdesk.auth.dto.ChangePasswordRequest;
import com.example.supportdesk.auth.dto.LoginRequest;
import com.example.supportdesk.auth.dto.LoginResponse;
import com.example.supportdesk.auth.dto.MeResponse;
import com.example.supportdesk.security.principal.AppUserPrincipal;
import com.example.supportdesk.security.service.JwtService;
import com.example.supportdesk.user.entity.AppUser;
import com.example.supportdesk.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    //
    public LoginResponse login(LoginRequest request){
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        request.username(),
                        request.password()
                )
        );
        //
        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
        String accessToken = jwtService.generateToken(principal);
        //
        return new LoginResponse(
                accessToken,
                "Bearer",
                jwtService.getAccessTokenExpirationMs()
        );
    }

    public MeResponse me(AppUserPrincipal principal){
        return MeResponse.fromPrincipal(principal);
    }

    public void changePassword(AppUserPrincipal principal, ChangePasswordRequest request){
        if (!request.newPassword().equals(request.confirmNewPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Passwords don't match"
            );
        }

        //
        AppUser user = appUserRepository.findById(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        //
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Current password is incorrect"
            );
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "New password must be different from current password"
            );
        }

        //
        user.changePassword(passwordEncoder.encode(request.newPassword()));
        appUserRepository.save(user);
    }
}
