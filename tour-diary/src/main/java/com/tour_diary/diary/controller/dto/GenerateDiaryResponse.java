package com.tour_diary.diary.controller.dto;

import com.tour_diary.tourism.RecommendedPlace;
import java.util.List;

public record GenerateDiaryResponse(
        String diaryId,
        String originalImageUrl,
        String generatedImageUrl,
        String title,
        String content,
        List<String> detectedObjects,
        List<RecommendedPlace> recommendedPlaces
) {
}
