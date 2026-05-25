package com.tour_diary.auth.service;

import org.springframework.stereotype.Component;

@Component
public class AuthUserResolver {

    private final AuthTokenService authTokenService;

    public AuthUserResolver(AuthTokenService authTokenService) {
        this.authTokenService = authTokenService;
    }

    public String optionalUserId(String authorization) {
        String token = bearerToken(authorization);
        if (token.isBlank()) {
            return "";
        }
        try {
            return authTokenService.verifyAndGetUserId(token);
        } catch (IllegalArgumentException ex) {
            return "";
        }
    }

    private String bearerToken(String authorization) {
        if (authorization == null) {
            return "";
        }
        String clean = authorization.trim();
        return clean.startsWith("Bearer ") ? clean.substring("Bearer ".length()).trim() : "";
    }
}
