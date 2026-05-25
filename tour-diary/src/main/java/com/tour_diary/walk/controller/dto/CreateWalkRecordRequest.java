package com.tour_diary.walk.controller.dto;

public record CreateWalkRecordRequest(
        String dogId,
        String originalImageUrl,
        String address,
        String weather,
        Double latitude,
        Double longitude
) {
}
