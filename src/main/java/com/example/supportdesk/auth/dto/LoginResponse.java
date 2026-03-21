package com.example.supportdesk.auth.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        Long expiresInMs
) {
}
