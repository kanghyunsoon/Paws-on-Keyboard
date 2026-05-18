package com.tour_diary.diary.domain;

import com.tour_diary.tourism.RecommendedPlace;
import java.time.Instant;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "diaries")
public record Diary(
        @Id String id,
        String walkRecordId,
        String dogId,
        String title,
        String content,
        DiaryEmotion emotion,
        List<String> detectedObjects,
        String diaryPrompt,
        String imagePrompt,
        String generatedImageUrl,
        List<RecommendedPlace> recommendedPlaces,
        Instant createdAt
) {
}
