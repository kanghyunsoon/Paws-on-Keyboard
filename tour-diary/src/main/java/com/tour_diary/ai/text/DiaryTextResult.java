package com.tour_diary.ai.text;

import com.tour_diary.diary.domain.DiaryEmotion;

public record DiaryTextResult(
        String title,
        String content,
        DiaryEmotion emotion
) {
}
