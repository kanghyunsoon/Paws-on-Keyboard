package com.tour_diary.diary.controller.dto;

public record DiaryDebugResponse(
        String visionResult,
        String diaryPrompt,
        String imagePrompt,
        String tourismPrompt,
        String rawTourismResponse
) {
}
