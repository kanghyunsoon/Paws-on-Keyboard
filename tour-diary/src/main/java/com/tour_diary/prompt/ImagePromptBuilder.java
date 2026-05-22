package com.tour_diary.prompt;

import com.tour_diary.ai.vision.VisionAnalysisResult;
import com.tour_diary.dog.domain.DogProfile;
import com.tour_diary.walk.domain.WalkRecord;
import org.springframework.stereotype.Component;

@Component
public class ImagePromptBuilder {

    public String build(DogProfile dog, WalkRecord walk, VisionAnalysisResult vision, String diaryContent) {
        return """
                Create only the upper drawing area of a Korean elementary-school picture diary.
                Do not draw any text, letters, handwriting, labels, title, date, signature, watermark, or notebook lines.
                The backend will add the Korean diary text separately.

                Diary content:
                "%s"

                Scene:
                The dog named %s is walking at %s.
                Photo analysis says the place is %s with this mood: %s.
                Important objects are: %s.
                Diary hints are: %s.

                Drawing style:
                - very naive childlike crayon and colored-pencil drawing
                - simple flat shapes, uneven outlines, playful mistakes, imperfect proportions
                - cute dog-centered scene from the dog's point of view
                - pastel colors with a few bright crayon accents
                - white paper background
                - slightly messy but wholesome elementary-school drawing feeling
                - looks like a young child drew it, not a professional illustrator

                Composition:
                Place the dog near the center, looking curious.
                Add small cute sticker-like elements around the scene: %s.
                Make it suitable for a pet diary album and a tourism contest demo.

                Avoid:
                - photorealistic rendering
                - any text, letters, fake letters, signature, or watermark
                - clean digital illustration
                - polished adult drawing
                - comic panels
                - realistic dog anatomy
                """.formatted(
                diaryContent,
                dog.name(),
                walk.address(),
                vision.placeType(),
                vision.mood(),
                vision.objects(),
                vision.diaryHints(),
                vision.stickerCandidates()
        );
    }
}
