package com.tour_diary.profile.controller.dto;

public record SaveProfileRequest(
        String userId,
        String userName,
        String dogName,
        Integer dogAge,
        String dogBreed,
        String dogAppearance,
        String dogFavoriteThings,
        String dogDislikedThings,
        String dogTraits,
        String ownerName,
        String ownerRole,
        String ownerNickname,
        String ownerGender,
        String relationshipNote,
        String dogPhotoUrl,
        String ownerPhotoUrl
) {
}
