package com.tour_diary.ai.image;

public interface AiImageService {

    String generateImage(String imagePrompt, String diaryTitle, String diaryContent);

    default String generateImage(String imagePrompt, String diaryTitle, String diaryContent, String referenceImageUrl) {
        return generateImage(imagePrompt, diaryTitle, diaryContent);
    }

    default String generateImage(
            String imagePrompt,
            String diaryTitle,
            String diaryContent,
            String referenceImageUrl,
            String dogReferenceImageUrl
    ) {
        return generateImage(imagePrompt, diaryTitle, diaryContent, referenceImageUrl);
    }
}
