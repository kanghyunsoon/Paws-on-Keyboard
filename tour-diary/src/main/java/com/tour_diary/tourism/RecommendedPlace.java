package com.tour_diary.tourism;

public record RecommendedPlace(
        String name,
        String reason,
        String category,
        String address,
        Double latitude,
        Double longitude
) {
}
