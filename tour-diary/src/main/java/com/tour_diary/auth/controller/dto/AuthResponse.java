package com.tour_diary.auth.controller.dto;

public record AuthResponse(
        String id,
        String email,
        String name,
        String token,
        String tokenExpiresAt
) {
}
