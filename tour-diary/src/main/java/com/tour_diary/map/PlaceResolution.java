package com.tour_diary.map;

public record PlaceResolution(
        String name,
        String address,
        Double latitude,
        Double longitude,
        String provider
) {
}
