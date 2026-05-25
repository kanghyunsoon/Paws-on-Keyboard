package com.tour_diary.community.controller.dto;

public record CreatePostRequest(
        String authorId,
        String authorName,
        String dogName,
        String dogPhotoPreview,
        String ownerPhotoPreview,
        String diaryId,
        String title,
        String content,
        String imagePreview,
        String place
) {
}
