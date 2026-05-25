package com.tour_diary.community.domain;

import java.time.Instant;

public record CommunityComment(
        String id,
        String authorId,
        String authorName,
        String content,
        Instant createdAt
) {
}
