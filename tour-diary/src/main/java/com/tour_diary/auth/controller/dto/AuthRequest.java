package com.tour_diary.auth.controller.dto;

public record AuthRequest(
        String email,
        String name,
        String password
) {
}
