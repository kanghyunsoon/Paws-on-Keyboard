package com.tour_diary.community.domain;

import java.time.Instant;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "community_posts")
public record CommunityPost(
        @Id String id,
        String authorId,
        String authorName,
        String dogName,
        String dogPhotoPreview,
        String ownerPhotoPreview,
        String diaryId,
        String title,
        String content,
        String imagePreview,
        String place,
        Instant createdAt,
        List<String> likes,
        List<CommunityComment> comments
) {
}
