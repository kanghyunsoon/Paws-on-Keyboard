package com.tour_diary.community.domain;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "follow_relations")
public record FollowRelation(
        @Id String id,
        @Indexed String followerId,
        @Indexed String followingId,
        Instant createdAt
) {
}
