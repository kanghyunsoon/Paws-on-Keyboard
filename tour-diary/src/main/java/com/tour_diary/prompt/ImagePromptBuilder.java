package com.tour_diary.prompt;

import com.tour_diary.ai.vision.VisionAnalysisResult;
import com.tour_diary.dog.domain.DogProfile;
import com.tour_diary.walk.domain.WalkRecord;
import org.springframework.stereotype.Component;

@Component
public class ImagePromptBuilder {

    public String build(DogProfile dog, WalkRecord walk, VisionAnalysisResult vision, String reconstructedStory) {
        return """
                Create one beautiful hand-drawn illustration for a dog picture diary.

                Pipeline contract:
                The user uploaded a real photo.
                The photo was analyzed into keywords.
                Those keywords were reconstructed into the dog-view story below.
                Draw the scene from that reconstructed story while preserving the real photo keywords and dog identity.

                Reconstructed dog-view story:
                "%s"

                Dog identity lock:
                Dog name: %s
                Age/personality/profile: %s
                Speaking style and owner relationship: %s
                Photo-derived dog appearance: %s
                Photo-derived colors: %s
                Keep the user's dog recognizable: coat colors, markings, breed impression, size, ears, face shape, tail, collar, and leash if visible.
                If the dog/profile/photo says black-and-white, white, cream, dark, or brown fur, preserve that fur family.
                Never transfer a bright background color onto the dog's fur.

                Photo keywords that must guide the drawing:
                Scene summary: %s
                Place type: %s
                Weather/location input: %s / %s
                Dog action: %s
                Dog viewpoint: %s
                Owner clue: %s
                Mood: %s
                Important objects: %s
                Drawing keywords: %s
                Diary hints: %s

                Required visual direction:
                - The drawing must feel similar to the real uploaded photo in scene, dog, color mood, and composition clues.
                - It is not a photo filter, not posterized, not a copied photo, and not photorealistic.
                - Draw it as the dog remembers the moment: low viewpoint, paws/ground feeling, leash/collar, smell-focused details, and looking toward the owner if owner clues exist.
                - Style is polished pastel crayon and colored-pencil illustration.
                - Cute, slightly crooked, warm, and childlike, but still pretty enough for a user to want to save and share.
                - Think "a talented 10-year-old carefully drew a beloved dog", not toddler scribbles.
                - Keep anatomy readable: clear dog face, eyes, ears, body, paws, and tail.
                - Use only objects and background elements visible in the reference photo or listed in the photo keywords.
                - Do not add text, letters, speech bubbles, date, watermark, diary page, notebook paper, UI, stamps, or frames inside the image.

                Negative constraints:
                wrong dog, wrong fur color, wrong breed, generic yellow dog, unrelated park, invented flowers, invented bench, invented season, extra limbs, human-like dog, scary face, messy low-quality scribble, text, watermark, diary template.
                """.formatted(
                reconstructedStory,
                dog.name(),
                dog.personality(),
                dog.speakingStyle(),
                vision.dogAppearance(),
                vision.colors(),
                vision.sceneSummary(),
                vision.placeType(),
                walk.weather(),
                walk.address(),
                vision.dogAction(),
                vision.dogViewpoint(),
                vision.ownerClue(),
                vision.mood(),
                vision.objects(),
                vision.drawingKeywords(),
                vision.diaryHints()
        );
    }
}
