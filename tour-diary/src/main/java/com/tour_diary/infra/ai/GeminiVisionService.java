package com.tour_diary.infra.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tour_diary.ai.vision.VisionAnalysisResult;
import com.tour_diary.ai.vision.VisionService;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Primary
public class GeminiVisionService implements VisionService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final FakeVisionService fallback;
    private final String apiKey;
    private final String model;

    public GeminiVisionService(
            FakeVisionService fallback,
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
    public VisionAnalysisResult analyze(String imageUrl) {
        if (apiKey == null || apiKey.isBlank() || imageUrl == null || imageUrl.isBlank()) {
            return fallback.analyze(imageUrl);
        }

        try {
            ImagePayload image = readImage(imageUrl);
            Map<String, Object> request = Map.of(
                    "contents", List.of(Map.of(
                            "parts", List.of(
                                    Map.of("text", visionPrompt()),
                                    Map.of("inline_data", Map.of(
                                            "mime_type", image.mimeType(),
                                            "data", Base64.getEncoder().encodeToString(image.bytes())
                                    ))
                            )
                    )),
                    "generationConfig", Map.of(
                            "responseMimeType", "application/json"
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
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return fallback.analyze(imageUrl);
        } catch (RuntimeException | IOException ex) {
            return fallback.analyze(imageUrl);
        }
    }

    private ImagePayload readImage(String imageUrl) throws IOException, InterruptedException {
        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            HttpResponse<byte[]> response = HttpClient.newHttpClient()
                    .send(HttpRequest.newBuilder(URI.create(imageUrl)).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
            String contentType = response.headers().firstValue("content-type").orElse("image/jpeg");
            return new ImagePayload(response.body(), contentType.split(";")[0]);
        }

        Path path = imageUrl.startsWith("/")
                ? Path.of("." + imageUrl).normalize()
                : Path.of(imageUrl).normalize();
        byte[] bytes = Files.readAllBytes(path);
        String mimeType = Files.probeContentType(path);
        return new ImagePayload(bytes, mimeType == null ? "image/jpeg" : mimeType);
    }

    private VisionAnalysisResult parseResponse(String response) throws IOException {
        JsonNode root = objectMapper.readTree(response);
        String content = root.path("candidates")
                .path(0)
                .path("content")
                .path("parts")
                .path(0)
                .path("text")
                .asText();
        JsonNode json = objectMapper.readTree(content);

        return new VisionAnalysisResult(
                json.path("dogExists").asBoolean(true),
                strings(json.path("objects"), List.of("강아지")),
                strings(json.path("colors"), List.of()),
                text(json, "mood", "차분한 산책"),
                text(json, "placeType", "산책 장소"),
                strings(json.path("stickerCandidates"), List.of("발자국")),
                strings(json.path("diaryHints"), List.of("오늘 산책에서 기억에 남은 장면"))
        );
    }

    private String visionPrompt() {
        return """
                산책 사진을 분석해서 아래 JSON만 반환해.
                {
                  "dogExists": true,
                  "objects": ["사진에서 보이는 핵심 객체 3~6개"],
                  "colors": ["주요 색감"],
                  "mood": "사진 분위기 한 문장",
                  "placeType": "공원, 산책로, 해변, 관광지 등 장소 유형",
                  "stickerCandidates": ["그림일기에 넣을 귀여운 요소"],
                  "diaryHints": ["강아지 시점 일기에 쓸 수 있는 상황 힌트"]
                }
                반려견 산책 일기와 관광지 추천에 쓸 정보만 간결하게 써.
                """;
    }

    private List<String> strings(JsonNode node, List<String> fallbackValue) {
        if (!node.isArray()) {
            return fallbackValue;
        }
        List<String> values = objectMapper.convertValue(
                node,
                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
        );
        return values.isEmpty() ? fallbackValue : values;
    }

    private String text(JsonNode node, String field, String fallbackValue) {
        String value = node.path(field).asText();
        return value == null || value.isBlank() ? fallbackValue : value;
    }

    private record ImagePayload(byte[] bytes, String mimeType) {
    }
}
