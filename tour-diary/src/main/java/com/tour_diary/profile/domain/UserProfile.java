package com.tour_diary.profile.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "user_profiles")
public record UserProfile(
        @Id String id,
        String userName,
        String dogId,
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
