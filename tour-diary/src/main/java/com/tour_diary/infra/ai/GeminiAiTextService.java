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

    public GeminiAiTextService(
            FakeAiTextService fallback,
            @Value("${app.ai.gemini-api-key:}") String apiKey,
            @Value("${app.ai.gemini-model:gemini-2.5-flash}") String model
    ) {
        this.restClient = RestClient.create();
        this.objectMapper = new ObjectMapper();
        this.fallback = fallback;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public DiaryTextResult generateDiary(String diaryPrompt) {
        if (apiKey == null || apiKey.isBlank()) {
            return fallback.generateDiary(diaryPrompt);
        }

        try {
            Map<String, Object> request = Map.of(
                    "contents", List.of(Map.of(
                            "parts", List.of(Map.of("text", diaryPrompt + """

                                    아래 JSON 스키마로만 답해.
                                    {
                                      "title": "일기 제목",
                                      "content": "5문장 이내 강아지 시점 일기",
                                      "emotion": "HAPPY|CURIOUS|EXCITED|CALM|NERVOUS"
                                    }
                                    """))
                    )),
                    "generationConfig", Map.of(
                            "responseMimeType", "application/json",
                            "temperature", 0.8
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
                text(json, "title", "오늘의 산책 일기"),
                text(json, "content", "오늘 산책은 조용하고 즐거웠어. 집사야, 다음에도 같이 가자!"),
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
