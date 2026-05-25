package com.tour_diary.community.controller.dto;

public record CreateCommentRequest(
        String authorId,
        String authorName,
        String content
) {
}
