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
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Primary
public class GroqAiTextService implements AiTextService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final GeminiAiTextService fallback;
    private final String apiKey;
    private final String model;
    private final boolean externalApiEnabled;

    public GroqAiTextService(
            GeminiAiTextService fallback,
            @Value("${app.ai.groq-api-key:}") String apiKey,
            @Value("${app.ai.groq-model:llama-3.1-8b-instant}") String model,
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
                    "model", model,
                    "temperature", 0.75,
                    "max_tokens", 700,
                    "response_format", Map.of("type", "json_object"),
                    "messages", List.of(
                            Map.of(
                                    "role", "system",
                                    "content", "You write Korean dog-view picture diary stories as JSON only. First use photo keywords, then adapt them to the dog's profile, age, and personality. Never invent visible facts not in the prompt."
                            ),
                            Map.of(
                                    "role", "user",
                                    "content", diaryPrompt + """

                                            Return JSON only with this schema:
                                            {
                                              "title": "short Korean picture diary title",
                                              "content": "5 to 7 Korean sentences written in first person as the dog. Use the extracted photo keywords and the dog's profile. Do not mention prompts or analysis.",
                                              "emotion": "HAPPY|CURIOUS|EXCITED|CALM|NERVOUS"
                                            }
                                            """
                            )
                    )
            );

            String response = restClient.post()
                    .uri("https://api.groq.com/openai/v1/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
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
        String content = root.path("choices")
                .path(0)
                .path("message")
                .path("content")
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
