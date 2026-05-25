package com.tour_diary.diary.controller.dto;

public record GenerateDiaryRequest(
        String userId,
        String dogId,
        String walkRecordId,
        String dogName,
        Integer dogAge,
        String dogBreed,
        String dogPhotoUrl,
        String dogGender,
        String dogPersonality,
        String favoriteThings,
        String dislikedThings,
        String ownerName,
        String ownerRole,
        String relationshipNote,
        String walkLocation,
        String walkWeather,
        String walkActivity,
        String diaryTone,
        String diaryPromptNotes,
        String imagePromptNotes
) {
}
