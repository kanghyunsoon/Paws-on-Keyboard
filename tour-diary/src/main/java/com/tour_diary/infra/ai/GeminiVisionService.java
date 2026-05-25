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
    private final boolean externalApiEnabled;

    public GeminiVisionService(
            FakeVisionService fallback,
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
    public VisionAnalysisResult analyze(String imageUrl) {
        if (!externalApiEnabled || apiKey == null || apiKey.isBlank() || imageUrl == null || imageUrl.isBlank()) {
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
                    "generationConfig", Map.of("responseMimeType", "application/json")
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
                strings(json.path("colors"), List.of("사진 속 실제 색감")),
                text(json, "mood", "차분한 산책 분위기"),
                text(json, "placeType", "산책 장소"),
                strings(json.path("stickerCandidates"), List.of("발자국")),
                strings(json.path("diaryHints"), List.of("강아지 시점 일기에 쓸 수 있는 사진 속 순간")),
                text(json, "sceneSummary", "사진 속 강아지가 산책 장소에 머문 순간"),
                text(json, "dogAppearance", "사진과 초기 설정에서 확인되는 강아지 외모"),
                text(json, "ownerClue", "보호자가 보이면 위치/손/리드줄 단서만 기록하고 보이지 않으면 보이지 않음"),
                text(json, "dogAction", "사진에서 보이는 강아지 자세와 행동"),
                text(json, "dogViewpoint", "강아지가 낮은 시점에서 냄새와 소리로 기억할 만한 장면"),
                strings(json.path("drawingKeywords"), List.of("강아지", "사진 속 배경", "산책 분위기"))
        );
    }

    private String visionPrompt() {
        return """
                Analyze the uploaded dog photo and return JSON only.
                The goal is not to caption the photo generally. The goal is to extract reliable keywords for a dog-view picture diary.

                Rules:
                - Only describe what is visible or strongly implied by the image.
                - Do not invent a famous place, season, flower, bench, owner gender, other people, or other animals.
                - Describe the dog appearance concretely: coat colors, markings, size, ears, face, tail, collar/leash if visible.
                - Extract scene nouns and relations that can help a drawing: dog position, ground material, background type, visible objects, owner clue.
                - Extract dog-view sensory hints: smell, paws, low viewpoint, leash direction, looking up at owner if visible.
                - Keep the output concise but specific.

                Return this exact JSON shape:
                {
                  "dogExists": true,
                  "objects": ["3 to 6 important visible objects in Korean"],
                  "colors": ["main real colors in Korean"],
                  "mood": "one Korean sentence about the photo mood",
                  "placeType": "park, paved path, cafe, indoor room, beach, waterside, street, or another visible place type in Korean",
                  "stickerCandidates": ["cute small visual elements suitable for diary decoration"],
                  "diaryHints": ["dog-view story hints based on the photo"],
                  "sceneSummary": "one Korean sentence summarizing the real photo situation",
                  "dogAppearance": "visible dog fur color, markings, size, ears, face, tail, collar/leash",
                  "ownerClue": "only visible owner clues; if not visible, say 보이지 않음",
                  "dogAction": "visible dog pose or action",
                  "dogViewpoint": "how the dog would remember this moment from low viewpoint, smell, paws, or leash",
                  "drawingKeywords": ["6 to 10 keywords that must appear in the drawing"]
                }
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
