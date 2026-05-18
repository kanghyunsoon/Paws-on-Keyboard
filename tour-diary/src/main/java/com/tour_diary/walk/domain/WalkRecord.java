package com.tour_diary.walk.domain;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "walk_records")
public record WalkRecord(
        @Id String id,
        String dogId,
        String originalImageUrl,
        Double latitude,
        Double longitude,
        String address,
        String weather,
        Double temperature,
        Instant walkedAt
) {
}
