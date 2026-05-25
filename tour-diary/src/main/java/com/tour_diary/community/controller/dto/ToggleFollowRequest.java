package com.tour_diary.community.controller.dto;

public record ToggleFollowRequest(
        String followerId,
        String followingId
) {
}
