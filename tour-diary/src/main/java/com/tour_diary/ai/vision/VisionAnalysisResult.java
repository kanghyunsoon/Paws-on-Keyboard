package com.tour_diary.ai.vision;

import java.util.List;

public record VisionAnalysisResult(
        boolean dogExists,
        List<String> objects,
        List<String> colors,
        String mood,
        String placeType,
        List<String> stickerCandidates,
        List<String> diaryHints,
        String sceneSummary,
        String dogAppearance,
        String ownerClue,
        String dogAction,
        String dogViewpoint,
        List<String> drawingKeywords
) {
}
