package com.tour_diary.community.controller.dto;

import java.util.List;

public record FollowSummaryResponse(
        List<String> followingIds,
        List<String> followerIds
) {
}
