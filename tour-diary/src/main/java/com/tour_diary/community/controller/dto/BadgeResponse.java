package com.tour_diary.community.controller.dto;

import java.time.Instant;

public record BadgeResponse(
        String userId,
        String postId,
        String title,
        String badgeImagePrompt,
        String dogPhotoPreview,
        String ownerPhotoPreview,
        Integer rank,
        String period,
        Instant awardedAt
) {
}
