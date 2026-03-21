package com.example.supportdesk.auth.controller;

import com.example.supportdesk.auth.dto.ChangePasswordRequest;
import com.example.supportdesk.auth.dto.LoginRequest;
import com.example.supportdesk.auth.dto.LoginResponse;
import com.example.supportdesk.auth.dto.MeResponse;
import com.example.supportdesk.auth.service.AuthService;
import com.example.supportdesk.security.principal.AppUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    //
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest loginRequest) {
        return authService.login(loginRequest);
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal AppUserPrincipal principal) {
        return authService.me(principal);
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest changePasswordRequest
    ) {
        authService.changePassword(principal, changePasswordRequest);
        return ResponseEntity.noContent().build();
    }
}
