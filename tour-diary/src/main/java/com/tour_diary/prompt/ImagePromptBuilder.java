package com.tour_diary.prompt;

import com.tour_diary.ai.vision.VisionAnalysisResult;
import com.tour_diary.dog.domain.DogProfile;
import com.tour_diary.walk.domain.WalkRecord;
import org.springframework.stereotype.Component;

@Component
public class ImagePromptBuilder {

    public String build(DogProfile dog, WalkRecord walk, VisionAnalysisResult vision, String diaryContent) {
        return """
                Create a cute hand-drawn crayon-style diary illustration from a dog's point of view.

                Diary content:
                "%s"

                Scene:
                The dog named %s is walking at %s.
                Photo analysis says the place is %s with this mood: %s.
                Important objects are: %s.
                Diary hints are: %s.

                Style:
                - childlike crayon drawing
                - pastel colors
                - slightly crooked lines
                - simple shapes
                - warm diary illustration
                - cute and imperfect
                - looks like a dog tried to draw its own walk
                - no realistic rendering
                - no photorealism
                - no scary mood

                Composition:
                Place the dog near the center, looking curious.
                Add small cute sticker-like elements around the scene: %s.
                Make it suitable for a pet diary album.
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
