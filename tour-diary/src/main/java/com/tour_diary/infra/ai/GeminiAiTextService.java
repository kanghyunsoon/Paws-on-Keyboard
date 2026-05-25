package com.tour_diary.infra.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tour_diary.ai.text.AiTextService;
import com.tour_diary.ai.text.DiaryTextResult;
import com.tour_diary.diary.domain.DiaryEmotion;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class GeminiAiTextService implements AiTextService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final FakeAiTextService fallback;
    private final String apiKey;
    private final String model;
    private final boolean externalApiEnabled;

    public GeminiAiTextService(
            FakeAiTextService fallback,
            @Value("${app.ai.gemini-api-key:}") String apiKey,
            @Value("${app.ai.gemini-model:gemini-2.5-flash}") String model,
            @Value("${app.external-api-enabled:true}") boolean externalApiEnabled
    ) {
        this.restClient = RestClient.create();
        this.objectMapper = new ObjectMapper();
        this.fallback = fallback;
        this.apiKey = apiKey;
        this.model = model;
        this.externalApiEnabled = externalApiEnabled;
    }

    @Override
    public DiaryTextResult generateDiary(String diaryPrompt) {
        if (!externalApiEnabled || apiKey == null || apiKey.isBlank()) {
            return fallback.generateDiary(diaryPrompt);
        }

        try {
            Map<String, Object> request = Map.of(
                    "contents", List.of(Map.of(
                            "parts", List.of(Map.of("text", diaryPrompt + """

                                    Return JSON only with this schema:
                                    {
                                      "title": "short Korean picture diary title",
                                      "content": "5 to 7 Korean sentences written in first person as the dog. Use the extracted photo keywords and the dog's profile. Do not mention prompts or analysis.",
                                      "emotion": "HAPPY|CURIOUS|EXCITED|CALM|NERVOUS"
                                    }
                                    """))
                    )),
                    "generationConfig", Map.of(
                            "responseMimeType", "application/json",
                            "temperature", 0.75
                    )
            );

            String response = restClient.post()
                    .uri("https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={key}",
                            model,
                            apiKey)
                    .body(request)
                    .retrieve()
                    .body(String.class);

            return parseResponse(response);
        } catch (RuntimeException | IOException ex) {
            return fallback.generateDiary(diaryPrompt);
        }
    }

    private DiaryTextResult parseResponse(String response) throws IOException {
        JsonNode root = objectMapper.readTree(response);
        String content = root.path("candidates")
                .path(0)
                .path("content")
                .path("parts")
                .path(0)
                .path("text")
                .asText();
        JsonNode json = objectMapper.readTree(content);

        return new DiaryTextResult(
                text(json, "title", "오늘의 산책 그림일기"),
                text(json, "content", "오늘 나는 보호자와 사진 속 산책길을 걸었다. 발바닥에 닿는 바닥 느낌과 코끝 냄새가 오래 남았다. 나는 보호자를 올려다보며 이 장면을 꼭 기억하고 싶었다."),
                emotion(text(json, "emotion", "CURIOUS"))
        );
    }

    private DiaryEmotion emotion(String value) {
        try {
            return DiaryEmotion.valueOf(value.toUpperCase());
        } catch (RuntimeException ex) {
            return DiaryEmotion.CURIOUS;
        }
    }

    private String text(JsonNode node, String field, String fallbackValue) {
        String value = node.path(field).asText();
        return value == null || value.isBlank() ? fallbackValue : value;
    }
}
