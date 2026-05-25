package com.tour_diary.diagnostics.controller;

import com.tour_diary.ai.image.AiImageService;
import com.tour_diary.ai.vision.VisionAnalysisResult;
import com.tour_diary.ai.vision.VisionService;
import com.tour_diary.diagnostics.controller.dto.DiagnosticsResponse;
import com.tour_diary.upload.controller.dto.UploadImageResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(value = "/api/diagnostics", produces = "application/json; charset=UTF-8")
public class DiagnosticsController {

    private final boolean externalApiEnabled;
    private final Map<String, String> values;
    private final AiImageService aiImageService;
    private final VisionService visionService;

    public DiagnosticsController(
            AiImageService aiImageService,
            VisionService visionService,
            @Value("${app.external-api-enabled:true}") boolean externalApiEnabled,
            @Value("${app.ai.gemini-api-key:}") String geminiApiKey,
            @Value("${app.ai.groq-api-key:}") String groqApiKey,
            @Value("${app.ai.cloudflare-account-id:}") String cloudflareAccountId,
            @Value("${app.ai.cloudflare-api-token:}") String cloudflareApiToken,
            @Value("${app.ai.huggingface-token:}") String huggingFaceToken,
            @Value("${app.ai.image-generate-url:}") String imageGenerateUrl,
            @Value("${app.kakao.rest-api-key:}") String kakaoRestApiKey,
            @Value("${app.kto.tour-api-key:}") String ktoTourApiKey,
            @Value("${app.kto.pet-tour-api-key:}") String ktoPetTourApiKey,
            @Value("${app.kto.durunubi-api-key:}") String ktoDurunubiApiKey,
            @Value("${app.kto.accessible-tour-api-key:}") String ktoAccessibleTourApiKey,
            @Value("${app.kto.green-tour-api-key:}") String ktoGreenTourApiKey,
            @Value("${app.weather.kma-api-key:}") String kmaWeatherApiKey,
            @Value("${spring.data.mongodb.uri:}") String mongoUri
    ) {
        this.aiImageService = aiImageService;
        this.visionService = visionService;
        this.externalApiEnabled = externalApiEnabled;
        this.values = new LinkedHashMap<>();
        values.put("gemini", geminiApiKey);
        values.put("groq", groqApiKey);
        values.put("cloudflareAccountId", cloudflareAccountId);
        values.put("cloudflareToken", cloudflareApiToken);
        values.put("huggingFace", huggingFaceToken);
        values.put("imageGenerateUrl", imageGenerateUrl);
        values.put("kakao", kakaoRestApiKey);
        values.put("ktoTour", ktoTourApiKey);
        values.put("ktoPetTour", ktoPetTourApiKey);
        values.put("ktoDurunubi", ktoDurunubiApiKey);
        values.put("ktoAccessibleTour", ktoAccessibleTourApiKey);
        values.put("ktoGreenTour", ktoGreenTourApiKey);
        values.put("kmaWeather", kmaWeatherApiKey);
        values.put("mongoUri", mongoUri);
    }

    @GetMapping
    public DiagnosticsResponse getDiagnostics() {
        Map<String, Boolean> configured = new LinkedHashMap<>();
        values.forEach((key, value) -> configured.put(key, value != null && !value.isBlank()));

        Map<String, String> notes = new LinkedHashMap<>();
        notes.put("secrets", "키 값은 응답에 노출하지 않습니다.");
        notes.put("vision", "사진에서 강아지 외모, 실제 색감, 장소 유형, 객체, 행동, 강아지 시점 단서를 먼저 추출합니다.");
        notes.put("story", "추출 키워드를 초기 설정의 강아지 성격과 나이에 맞춰 강아지 1인칭 이야기로 재구성합니다.");
        notes.put("image", "재구성한 이야기를 그림 프롬프트에 넣고, 실제 사진과 비슷한 장면/색감/강아지 정체성을 유지하도록 image-to-image를 우선 시도합니다.");
        notes.put("fallback", "외부 이미지 생성이 실패하면 사진 필터가 아닌 색상/장면 키워드 기반 로컬 강아지 기억 일러스트로 fallback합니다.");

        return new DiagnosticsResponse(externalApiEnabled, configured, notes);
    }

    @PostMapping(value = "/vision-test", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public VisionAnalysisResult testVision(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("테스트할 이미지가 비어 있습니다.");
        }

        Path directory = Path.of("uploads", "diagnostics").toAbsolutePath().normalize();
        Files.createDirectories(directory);
        String filename = "vision-reference-" + Instant.now().toEpochMilli() + extensionOf(file.getOriginalFilename());
        Path target = directory.resolve(filename).normalize();
        file.transferTo(target);

        return visionService.analyze("/uploads/diagnostics/" + filename);
    }

    @PostMapping(value = "/image-generation-test", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadImageResponse testImageGeneration(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("테스트할 이미지가 비어 있습니다.");
        }

        Path directory = Path.of("uploads", "diagnostics").toAbsolutePath().normalize();
        Files.createDirectories(directory);
        String filename = "reference-" + Instant.now().toEpochMilli() + extensionOf(file.getOriginalFilename());
        Path target = directory.resolve(filename).normalize();
        file.transferTo(target);

        String referenceUrl = "/uploads/diagnostics/" + filename;
        String prompt = """
                Analyze the uploaded dog photo as a real moment.
                Extract photo keywords first, then draw a polished pastel crayon and colored-pencil dog diary illustration from the reconstructed dog-view story.
                Preserve the exact dog identity from the reference photo keywords: same coat color, markings, breed impression, body shape, ears, face, tail, collar, and leash if visible.
                Keep the scene similar to the actual photo in color mood, composition clues, ground, and background.
                Make it cute and slightly crooked, like a talented 10-year-old carefully drew a beloved dog, but still pretty and shareable.
                Do not add text, letters, speech bubbles, diary page, notebook form, date boxes, fake flowers, or unrelated props.
                """;
        String generatedUrl = aiImageService.generateImage(prompt, "이미지 생성 테스트", "사진 기반 그림 생성 테스트", referenceUrl);
        return new UploadImageResponse("generated", generatedUrl);
    }

    private String extensionOf(String originalFilename) {
        if (originalFilename == null) {
            return ".jpg";
        }
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == originalFilename.length() - 1) {
            return ".jpg";
        }
        String extension = originalFilename.substring(dotIndex).toLowerCase(Locale.ROOT);
        return extension.matches("\\.(jpg|jpeg|png|webp)") ? extension : ".jpg";
    }
}
