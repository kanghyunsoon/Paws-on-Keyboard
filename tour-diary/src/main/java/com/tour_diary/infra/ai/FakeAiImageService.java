package com.tour_diary.infra.ai;

import com.tour_diary.ai.image.AiImageService;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.stereotype.Service;

@Service
public class FakeAiImageService implements AiImageService {

    @Override
    public String generateImage(String imagePrompt) {
        String key = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(imagePrompt.substring(0, Math.min(imagePrompt.length(), 64)).getBytes(StandardCharsets.UTF_8));
        return "/uploads/generated/diary-preview-" + key + ".png";
    }
}
