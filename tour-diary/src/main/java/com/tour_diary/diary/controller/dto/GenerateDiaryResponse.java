package com.tour_diary.diary.controller.dto;

import com.tour_diary.tourism.RecommendedPlace;
import java.time.Instant;
import java.util.List;

public record GenerateDiaryResponse(
        String diaryId,
        String userId,
        String dogId,
        String walkRecordId,
        String originalImageUrl,
        String generatedImageUrl,
        String title,
        String content,
        List<String> detectedObjects,
        List<RecommendedPlace> recommendedPlaces,
        Instant createdAt
) {
}
