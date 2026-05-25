package com.tour_diary.infra.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tour_diary.ai.image.AiImageService;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Primary
public class CloudflareAiImageService implements AiImageService {

    private static final Logger log = LoggerFactory.getLogger(CloudflareAiImageService.class);

    private static final int PAGE_WIDTH = 900;
    private static final int PAGE_HEIGHT = 1280;
    private static final int MARGIN = 52;
    private static final int DRAWING_TOP = 150;
    private static final int DRAWING_HEIGHT = 560;
    private static final int TEXT_TOP = 770;
    private static final int LINE_HEIGHT = 42;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final FakeAiImageService fallback;
    private final String accountId;
    private final String apiToken;
    private final String model;
    private final String cloudflareImageToImageModel;
    private final String geminiApiKey;
    private final String geminiImageModel;
    private final String huggingFaceToken;
    private final String huggingFaceImageToImageModel;
    private final String huggingFaceProvider;
    private final String imageGenerateUrl;
    private final boolean publicImageFallbackEnabled;
    private final boolean externalApiEnabled;
    private final boolean cloudflareImageToImageEnabled;
    private final HttpClient httpClient;

    public CloudflareAiImageService(
            FakeAiImageService fallback,
            @Value("${app.ai.cloudflare-account-id:}") String accountId,
            @Value("${app.ai.cloudflare-api-token:}") String apiToken,
            @Value("${app.ai.cloudflare-image-model:@cf/black-forest-labs/flux-1-schnell}") String model,
            @Value("${app.ai.cloudflare-image-to-image-model:@cf/runwayml/stable-diffusion-v1-5-img2img}") String cloudflareImageToImageModel,
            @Value("${app.ai.gemini-api-key:}") String geminiApiKey,
            @Value("${app.ai.gemini-image-model:gemini-3.1-flash-image-preview}") String geminiImageModel,
            @Value("${app.ai.huggingface-token:}") String huggingFaceToken,
            @Value("${app.ai.huggingface-image-to-image-model:black-forest-labs/FLUX.2-dev}") String huggingFaceImageToImageModel,
            @Value("${app.ai.huggingface-provider:fal-ai}") String huggingFaceProvider,
            @Value("${app.ai.image-generate-url:}") String imageGenerateUrl,
            @Value("${app.ai.public-image-fallback-enabled:true}") boolean publicImageFallbackEnabled,
            @Value("${app.ai.cloudflare-image-to-image-enabled:false}") boolean cloudflareImageToImageEnabled,
            @Value("${app.external-api-enabled:true}") boolean externalApiEnabled
    ) {
        this.restClient = RestClient.create();
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newHttpClient();
        this.fallback = fallback;
        this.accountId = accountId;
        this.apiToken = apiToken;
        this.model = model;
        this.cloudflareImageToImageModel = cloudflareImageToImageModel;
        this.geminiApiKey = geminiApiKey;
        this.geminiImageModel = geminiImageModel;
        this.huggingFaceToken = huggingFaceToken;
        this.huggingFaceImageToImageModel = huggingFaceImageToImageModel;
        this.huggingFaceProvider = huggingFaceProvider;
        this.imageGenerateUrl = imageGenerateUrl;
        this.publicImageFallbackEnabled = publicImageFallbackEnabled;
        this.cloudflareImageToImageEnabled = cloudflareImageToImageEnabled;
        this.externalApiEnabled = externalApiEnabled;
    }

    @Override
    public String generateImage(String imagePrompt, String diaryTitle, String diaryContent) {
        return generateImage(imagePrompt, diaryTitle, diaryContent, null, null);
    }

    @Override
    public String generateImage(String imagePrompt, String diaryTitle, String diaryContent, String referenceImageUrl) {
        return generateImage(imagePrompt, diaryTitle, diaryContent, referenceImageUrl, null);
    }

    @Override
    public String generateImage(
            String imagePrompt,
            String diaryTitle,
            String diaryContent,
            String referenceImageUrl,
            String dogReferenceImageUrl
    ) {
        String effectivePrompt = hasText(dogReferenceImageUrl)
                ? imagePrompt + "\n\nDog identity reference is registered from initial setup. Lock dog fur color, markings, breed impression, ears, face, body shape, and collar from that dog profile photo. Use the walk photo only for the situation and background."
                : imagePrompt;
        if (!externalApiEnabled) {
            return referencePreservingFallback(referenceImageUrl, dogReferenceImageUrl, diaryTitle, diaryContent, effectivePrompt);
        }

        try {
            if (hasText(referenceImageUrl) && hasText(geminiApiKey)) {
                try {
                    byte[] geminiImageBytes = generateGeminiImageEdit(referenceImageUrl, dogReferenceImageUrl, effectivePrompt);
                    if (geminiImageBytes.length > 0) {
                        log.info("Generated diary image through Gemini image edit model={}", geminiImageModel);
                        return saveImage(geminiImageBytes);
                    }
                    log.warn("Gemini image edit returned no usable image. Trying Hugging Face image-to-image.");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw ex;
                } catch (RuntimeException | IOException ex) {
                    log.warn("Gemini image edit failed. Trying next image provider. reason={}", ex.getMessage());
                }
            }

            if (hasText(referenceImageUrl) && hasText(huggingFaceToken)) {
                try {
                    byte[] imageToImageBytes = generateHuggingFaceImageToImage(referenceImageUrl, effectivePrompt);
                    if (imageToImageBytes.length > 0) {
                        log.info("Generated diary image through Hugging Face image-to-image model={}", huggingFaceImageToImageModel);
                        return saveImage(imageToImageBytes);
                    }
                    log.warn("Hugging Face image-to-image returned no usable image. Trying Cloudflare image-to-image.");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw ex;
                } catch (RuntimeException | IOException ex) {
                    log.warn("Hugging Face image-to-image failed. Trying next image provider. reason={}", ex.getMessage());
                }
            }

            if (hasText(referenceImageUrl) && cloudflareImageToImageEnabled && hasText(accountId) && hasText(apiToken)) {
                try {
                    byte[] cloudflareImageToImageBytes = generateCloudflareImageToImage(referenceImageUrl, effectivePrompt);
                    if (cloudflareImageToImageBytes.length > 0) {
                        log.info("Generated diary image through Cloudflare image-to-image model={}", cloudflareImageToImageModel);
                        return saveImage(blendReferenceIdentity(cloudflareImageToImageBytes, hasText(dogReferenceImageUrl) ? dogReferenceImageUrl : referenceImageUrl));
                    }
                    log.warn("Cloudflare image-to-image returned no usable image. Trying next image provider.");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw ex;
                } catch (RuntimeException | IOException ex) {
                    log.warn("Cloudflare image-to-image failed. Trying next image provider. reason={}", ex.getMessage());
                }
            }

            if (hasText(imageGenerateUrl)) {
                try {
                    byte[] configuredImageBytes = generateConfiguredImage(effectivePrompt);
                    if (configuredImageBytes.length > 0) {
                        log.info("Generated diary image through configured image generator url={}", imageGenerateUrl);
                        return saveImage(configuredImageBytes);
                    }
                    log.warn("Configured image generator returned no usable image. Trying public image fallback.");
                } catch (RuntimeException | IOException | InterruptedException ex) {
                    log.warn("Configured image generator failed. Trying public image fallback. reason={}", ex.getMessage());
                }
            }

            if (publicImageFallbackEnabled) {
                try {
                    byte[] publicImageBytes = generatePublicTextToImage(effectivePrompt);
                    if (publicImageBytes.length > 0) {
                        log.info("Generated diary image through public no-token image fallback.");
                        return saveImage(publicImageBytes);
                    }
                    log.warn("Public image fallback returned no usable image. Falling back to local memory illustration.");
                } catch (RuntimeException | IOException | InterruptedException ex) {
                    log.warn("Public image fallback failed. Falling back to local memory illustration. reason={}", ex.getMessage());
                }
            }

            if (hasText(accountId) && hasText(apiToken)) {
                try {
                    byte[] textToImageBytes = generateCloudflareTextToImage(effectivePrompt);
                    if (textToImageBytes.length > 0) {
                        log.info("Generated diary image through Cloudflare text-to-image model={} after no-token fallbacks failed.", model);
                        return saveImage(textToImageBytes);
                    }
                    log.warn("Cloudflare text-to-image returned no usable image. Falling back to local memory illustration.");
                } catch (RuntimeException | IOException ex) {
                    log.warn("Cloudflare text-to-image failed. Falling back to local memory illustration. reason={}", ex.getMessage());
                }
            }

            if (hasText(referenceImageUrl)) {
                log.warn("No remote image-to-image provider is available. Using local memory illustration.");
                return saveImage(renderLocalMemoryIllustration(referenceImageUrl, dogReferenceImageUrl, effectivePrompt));
            }

            if (!hasText(accountId) || !hasText(apiToken)) {
                return fallback.generateImage(imagePrompt, diaryTitle, diaryContent);
            }

            byte[] drawingBytes = generateCloudflareTextToImage(effectivePrompt);
            if (drawingBytes.length == 0) {
                log.warn("Cloudflare text-to-image returned no usable image. Falling back to local image.");
                return fallback.generateImage(imagePrompt, diaryTitle, diaryContent);
            }

            log.info("Generated diary image through Cloudflare text-to-image model={}", model);
            return saveImage(drawingBytes);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("Image generation interrupted. Falling back to local image.");
            return referencePreservingFallback(referenceImageUrl, dogReferenceImageUrl, diaryTitle, diaryContent, effectivePrompt);
        } catch (RuntimeException | IOException ex) {
            log.warn("Image generation failed. Falling back to local image. reason={}", ex.getMessage());
            return referencePreservingFallback(referenceImageUrl, dogReferenceImageUrl, diaryTitle, diaryContent, effectivePrompt);
        }
    }

    private byte[] generateGeminiImageEdit(String referenceImageUrl, String dogReferenceImageUrl, String imagePrompt) throws IOException, InterruptedException {
        ImagePayload walkImage = readImage(referenceImageUrl);
        ImagePayload dogImage = hasText(dogReferenceImageUrl) ? readImage(dogReferenceImageUrl) : null;
        String prompt = """
                Use the first uploaded image as the real walk/situation reference.
                If a second uploaded image exists, use it as the fixed dog identity reference from initial setup.
                Create a new charming hand-drawn pastel crayon and colored-pencil illustration of that moment from the dog diary prompt.
                This must look newly drawn by hand from the dog's memory, not like a photo filter, not pixel art, not a posterized or mosaic photo.
                Preserve the user's dog identity from the dog profile image: fur colors, markings, breed impression, ears, face, body shape, and collar.
                Use the walk image for place, weather, activity, pose clues, leash/collar if visible, owner clues if visible, and the photographed situation.
                Do not add text, letters, speech bubbles, frames, diary pages, notebook paper, date boxes, stamps, or unrelated props.

                Service-specific instructions:
                %s
                """.formatted(imagePrompt);
        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("text", prompt));
        parts.add(Map.of("inlineData", Map.of(
                "mimeType", walkImage.mimeType(),
                "data", Base64.getEncoder().encodeToString(walkImage.bytes())
        )));
        if (dogImage != null) {
            parts.add(Map.of("inlineData", Map.of(
                    "mimeType", dogImage.mimeType(),
                    "data", Base64.getEncoder().encodeToString(dogImage.bytes())
            )));
        }
        String jsonBody = objectMapper.writeValueAsString(Map.of(
                "contents", List.of(Map.of(
                        "parts", parts
                ))
        ));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/" + geminiImageModel + ":generateContent"))
                .header("x-goog-api-key", geminiApiKey)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.warn("Gemini image edit failed status={} body={}", response.statusCode(), safeBody(response.body()));
            return new byte[0];
        }
        return extractGeminiImageBytes(response.body());
    }

    private byte[] generateCloudflareImageToImage(String referenceImageUrl, String imagePrompt) throws IOException, InterruptedException {
        ImagePayload image = readImage(referenceImageUrl);
        String prompt = """
                Use the uploaded dog photo as situation reference and create a clean pastel colored-pencil illustration made by a talented 10-year-old child.
                It must be a newly hand-drawn dog-memory scene, not a photo filter, not pixel art, not a posterized photo.
                Keep the user's dog identity, fur markings, pose clues, collar/leash, owner clues if visible, and the photo-derived situation keywords.

                %s
                """.formatted(imagePrompt);
        String jsonBody = objectMapper.writeValueAsString(Map.of(
                "prompt", prompt,
                "negative_prompt", "pixel art, mosaic, posterized photo, low quality filter, blurry, text, letters, diary page, notebook, wrong dog, yellow dog, tan dog, brown dog, wrong breed, different scene",
                "image_b64", Base64.getEncoder().encodeToString(image.bytes()),
                "strength", 0.46,
                "guidance", 9.0,
                "num_steps", 20,
                "width", 768,
                "height", 768
        ));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.cloudflare.com/client/v4/accounts/" + accountId + "/ai/run/" + cloudflareImageToImageModel))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.warn("Cloudflare image-to-image failed status={} body={}", response.statusCode(), safeBody(response.body()));
            return new byte[0];
        }
        return extractImageBytes(response.body());
    }

    private byte[] generateCloudflareTextToImage(String imagePrompt) throws IOException {
        String compactPrompt = compactCloudflarePrompt(imagePrompt);
        String prompt = """
                Create a simple Korean elementary-school dog picture diary drawing.
                The result should feel cute, hand-drawn, and personal, not like a professional illustration.
                Follow the dog identity and photo-derived scene details in the prompt.
                Use full-color crayon and colored-pencil strokes, rough dark crayon outlines, off-white paper, slightly crooked shapes, simple perspective, and muted childlike colors.
                The dog may be black-and-white, but the whole image must be colored, not grayscale, not graphite, not charcoal, not monochrome.
                Do not create a polished mascot, glossy digital art, photorealistic painting, or generic front-facing dog portrait.
                Do not draw text, letters, speech bubbles, notebook paper, diary form, date boxes, or watermark.

                %s
                """.formatted(compactPrompt);
        byte[] responseBytes = restClient.post()
                .uri("https://api.cloudflare.com/client/v4/accounts/" + accountId + "/ai/run/" + model)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
                .body(Map.of("prompt", prompt))
                .retrieve()
                .body(byte[].class);
        return extractImageBytes(responseBytes);
    }

    private byte[] generateConfiguredImage(String imagePrompt) throws IOException, InterruptedException {
        String compactPrompt = compactCloudflarePrompt(imagePrompt);
        String jsonBody = objectMapper.writeValueAsString(Map.of(
                "prompt", compactPrompt,
                "negative_prompt", "text, watermark, signature, brown dog, yellow dog, tan dog, wrong breed, generic puppy",
                "width", 1024,
                "height", 1024,
                "steps", 28
        ));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(imageGenerateUrl))
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.warn("Configured image generator failed status={} body={}", response.statusCode(), safeBody(response.body()));
            return new byte[0];
        }
        return extractImageBytes(response.body());
    }

    private byte[] generatePublicTextToImage(String imagePrompt) throws IOException, InterruptedException {
        String compactPrompt = compactCloudflarePrompt(imagePrompt);
        String encodedPrompt = URLEncoder.encode(compactPrompt, StandardCharsets.UTF_8);
        String url = "https://image.pollinations.ai/prompt/" + encodedPrompt
                + "?width=1024&height=768&nologo=true&enhance=false&model=flux&seed=" + Math.abs(compactPrompt.hashCode());
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "image/png,image/jpeg,*/*")
                .GET()
                .build();
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        String contentType = response.headers().firstValue("content-type").orElse("");
        if (response.statusCode() < 200 || response.statusCode() >= 300 || !contentType.startsWith("image/")) {
            log.warn("Public image fallback failed status={} contentType={} body={}", response.statusCode(), contentType, safeBody(response.body()));
            return new byte[0];
        }
        return response.body();
    }

    private String compactCloudflarePrompt(String sourcePrompt) {
        String source = sourcePrompt == null ? "" : sourcePrompt;
        String lower = source.toLowerCase();
        boolean borderCollie = lower.contains("border collie") || source.contains("보더콜리");
        boolean blackWhite = borderCollie || ((lower.contains("black") || source.contains("검정") || source.contains("검은") || source.contains("검흰"))
                && (lower.contains("white") || source.contains("흰") || source.contains("하얀")));
        boolean dryField = lower.contains("dry grass") || lower.contains("field") || lower.contains("muddy")
                || source.contains("마른 풀") || source.contains("들판") || source.contains("흙길") || source.contains("갈대");
        boolean foggy = lower.contains("fog") || lower.contains("gray sky") || source.contains("안개") || source.contains("흐린");
        boolean paved = lower.contains("paved") || lower.contains("walkway") || source.contains("산책로") || source.contains("포장") || source.contains("보도");
        boolean whiteDog = !blackWhite && (lower.contains("white") || source.contains("하얀") || source.contains("흰"));

        String dog = blackWhite
                ? "a pure black-and-white border collie, black ears and black face patches, white muzzle and white chest, medium athletic herding dog body, tongue out, friendly expression, absolutely no tan markings and no brown eyebrows"
                : whiteDog
                ? "a white fluffy small dog, soft round face, white fur, cute friendly expression"
                : "the user's dog with the exact fur color and breed described in the source photo";
        String scene = dryField
                ? "walking toward the viewer on a narrow muddy path through tall dry grass and reeds"
                : paved
                ? "walking on a paved park path with the real photo situation preserved"
                : "in the real outdoor walk scene from the uploaded photo";
        String weather = foggy ? "foggy gray sky, soft muted light" : "soft natural daylight";

        return """
                Subject: %s.
                Scene: %s, %s.
                Viewpoint: dog-eye view from the dog's memory; include the dog's front paws at the bottom if natural; the world should feel big from a dog's low viewpoint.
                Style: full-color Korean elementary-school picture diary drawing, drawn by a talented 9 or 10 year old child, rough dark crayon outline, colored-pencil hatching, visible crayon strokes, off-white paper texture, simple cute shapes, slightly uneven lines, not too detailed.
                Color palette: clearly colored background, black-and-white dog only, warm tan dry grass, pale gray-blue cloudy sky, muted green weeds, light brown muddy path, tiny blue-gray distant houses. Do not make the whole image grayscale.
                Composition: simple wide diary drawing, lots of reeds and path, small distant houses if visible, muted gray sky, warm dry grass, charming but not professional.
                Strict negatives: no grayscale-only image, no monochrome drawing, no graphite pencil sketch, no charcoal sketch, no black-and-white-only background, no photorealistic painting, no glossy digital illustration, no professional concept art, no hyper-detailed fur, no 3D render, no brown dog, no yellow dog, no tan dog, no tan eyebrows, no brown cheek markings, no golden retriever, no beagle, no generic puppy, no text, no signature, no watermark, no diary page.
                """.formatted(dog, scene, weather);
    }

    private byte[] blendReferenceIdentity(byte[] generatedBytes, String referenceImageUrl) throws IOException, InterruptedException {
        BufferedImage generated = ImageIO.read(new ByteArrayInputStream(generatedBytes));
        BufferedImage reference = ImageIO.read(new ByteArrayInputStream(readImage(referenceImageUrl).bytes()));
        if (generated == null || reference == null) {
            return generatedBytes;
        }

        BufferedImage scaledReference = new BufferedImage(generated.getWidth(), generated.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaledReference.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(reference, 0, 0, generated.getWidth(), generated.getHeight(), null);
        } finally {
            g.dispose();
        }

        BufferedImage output = new BufferedImage(generated.getWidth(), generated.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < generated.getHeight(); y++) {
            for (int x = 0; x < generated.getWidth(); x++) {
                Color gen = new Color(generated.getRGB(x, y));
                Color ref = new Color(scaledReference.getRGB(x, y));
                int refBrightness = brightness(ref.getRGB());
                int refSpread = Math.max(ref.getRed(), Math.max(ref.getGreen(), ref.getBlue()))
                        - Math.min(ref.getRed(), Math.min(ref.getGreen(), ref.getBlue()));
                boolean likelyBlackWhiteDogPixel = refSpread < 34 && (refBrightness < 92 || refBrightness > 172);
                double refWeight = likelyBlackWhiteDogPixel ? 0.68 : 0.26;
                double genWeight = 1.0 - refWeight;

                int r = clampToByte(gen.getRed() * genWeight + ref.getRed() * refWeight);
                int green = clampToByte(gen.getGreen() * genWeight + ref.getGreen() * refWeight);
                int b = clampToByte(gen.getBlue() * genWeight + ref.getBlue() * refWeight);

                if (likelyBlackWhiteDogPixel) {
                    int gray = (int) Math.round(r * 0.299 + green * 0.587 + b * 0.114);
                    r = clampToByte(gray * 0.92 + ref.getRed() * 0.08);
                    green = clampToByte(gray * 0.92 + ref.getGreen() * 0.08);
                    b = clampToByte(gray * 0.92 + ref.getBlue() * 0.08);
                }

                output.setRGB(x, y, new Color(r, green, b).getRGB());
            }
        }

        return toPng(output);
    }

    private byte[] generateHuggingFaceImageToImage(String referenceImageUrl, String imagePrompt) throws IOException, InterruptedException {
        ImagePayload image = readImage(referenceImageUrl);
        String jsonBody = objectMapper.writeValueAsString(Map.of(
                "image_url", dataUrl(image),
                "image_urls", List.of(dataUrl(image)),
                "prompt", imagePrompt,
                "negative_prompt", "text, letters, watermark, fake diary page, school diary form, notebook paper, grids, date boxes, name boxes, wrong dog, wrong fur color, wrong breed, yellow dog, tan dog, brown dog, red fur, orange fur, extra limbs, human dog",
                "guidance_scale", 6.0,
                "num_inference_steps", 30,
                "image_size", Map.of("width", 1024, "height", 768)
        ));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(huggingFaceImageToImageUrl()))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + huggingFaceToken)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.warn("Hugging Face image-to-image failed status={} body={}", response.statusCode(), safeBody(response.body()));
            return new byte[0];
        }
        return extractHuggingFaceQueueImage(response.body());
    }

    private String huggingFaceImageToImageUrl() {
        String provider = hasText(huggingFaceProvider) ? huggingFaceProvider : "fal-ai";
        return "https://router.huggingface.co/" + provider + "/" + mappedFalImageModel() + "?_subdomain=queue";
    }

    private String mappedFalImageModel() {
        return switch (huggingFaceImageToImageModel) {
            case "black-forest-labs/FLUX.2-dev" -> "fal-ai/flux-2/edit";
            case "black-forest-labs/FLUX.1-Kontext-dev" -> "fal-ai/flux-pro/kontext/dev";
            default -> huggingFaceImageToImageModel;
        };
    }

    private byte[] extractHuggingFaceQueueImage(byte[] responseBody) throws IOException, InterruptedException {
        byte[] immediateImage = extractImageBytes(responseBody);
        if (immediateImage.length > 0) {
            return immediateImage;
        }

        JsonNode root = objectMapper.readTree(responseBody);
        String requestId = root.path("request_id").asText();
        String responseUrl = root.path("response_url").asText();
        if (requestId.isBlank() || responseUrl.isBlank()) {
            log.warn("Hugging Face queue response had no request id. body={}", safeBody(responseBody));
            return new byte[0];
        }

        URI responseUri = URI.create(responseUrl);
        String query = huggingFaceImageToImageUrl().contains("?")
                ? huggingFaceImageToImageUrl().substring(huggingFaceImageToImageUrl().indexOf('?'))
                : "";
        String baseUrl = "https://router.huggingface.co/" + (hasText(huggingFaceProvider) ? huggingFaceProvider : "fal-ai");
        String statusUrl = baseUrl + responseUri.getPath() + "/status" + query;
        String resultUrl = baseUrl + responseUri.getPath() + query;

        for (int attempt = 0; attempt < 180; attempt++) {
            Thread.sleep(500);
            HttpResponse<byte[]> statusResponse = authorizedGet(statusUrl);
            if (statusResponse.statusCode() < 200 || statusResponse.statusCode() >= 300) {
                log.warn("Hugging Face queue status failed status={} body={}", statusResponse.statusCode(), safeBody(statusResponse.body()));
                return new byte[0];
            }

            JsonNode statusRoot = objectMapper.readTree(statusResponse.body());
            String status = statusRoot.path("status").asText();
            if ("COMPLETED".equals(status)) {
                HttpResponse<byte[]> resultResponse = authorizedGet(resultUrl);
                if (resultResponse.statusCode() < 200 || resultResponse.statusCode() >= 300) {
                    log.warn("Hugging Face queue result failed status={} body={}", resultResponse.statusCode(), safeBody(resultResponse.body()));
                    return new byte[0];
                }
                return extractImageBytes(resultResponse.body());
            }
            if ("FAILED".equals(status) || "ERROR".equals(status)) {
                log.warn("Hugging Face queue generation failed. body={}", safeBody(statusResponse.body()));
                return new byte[0];
            }
        }

        log.warn("Hugging Face queue generation timed out.");
        return new byte[0];
    }

    private HttpResponse<byte[]> authorizedGet(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + huggingFaceToken)
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    private String dataUrl(ImagePayload image) {
        return "data:" + image.mimeType() + ";base64," + Base64.getEncoder().encodeToString(image.bytes());
    }

    private ImagePayload readImage(String imageUrl) throws IOException, InterruptedException {
        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            HttpResponse<byte[]> response = httpClient
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

    private byte[] extractImageBytes(byte[] responseBytes) throws IOException {
        if (responseBytes == null || responseBytes.length == 0) {
            return new byte[0];
        }

        if (looksLikeJson(responseBytes)) {
            JsonNode root = objectMapper.readTree(responseBytes);
            String base64Image = root.path("result").path("image").asText();
            if (base64Image.isBlank()) {
                base64Image = root.path("image").asText();
            }
            if (!base64Image.isBlank()) {
                return Base64.getDecoder().decode(stripDataUrlPrefix(base64Image));
            }

            JsonNode images = root.path("images");
            if (images.isArray() && !images.isEmpty() && images.get(0).isTextual()) {
                String firstImage = images.get(0).asText();
                if (!firstImage.startsWith("http://") && !firstImage.startsWith("https://")) {
                    return Base64.getDecoder().decode(stripDataUrlPrefix(firstImage));
                }
            }

            String imageUrl = firstImageUrl(root);
            if (!imageUrl.isBlank()) {
                try {
                    return readImage(imageUrl).bytes();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return new byte[0];
                }
            }
            return new byte[0];
        }

        return responseBytes;
    }

    private byte[] extractGeminiImageBytes(byte[] responseBytes) throws IOException {
        JsonNode root = objectMapper.readTree(responseBytes);
        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            return new byte[0];
        }

        JsonNode parts = candidates.get(0).path("content").path("parts");
        if (!parts.isArray()) {
            return new byte[0];
        }

        for (JsonNode part : parts) {
            String data = part.path("inlineData").path("data").asText();
            if (!data.isBlank()) {
                return Base64.getDecoder().decode(stripDataUrlPrefix(data));
            }
        }
        return new byte[0];
    }

    private String referencePreservingFallback(
            String referenceImageUrl,
            String dogReferenceImageUrl,
            String diaryTitle,
            String diaryContent,
            String imagePrompt
    ) {
        if (hasText(referenceImageUrl)) {
            try {
                return saveImage(renderLocalMemoryIllustration(referenceImageUrl, dogReferenceImageUrl, imagePrompt));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                log.warn("Local memory illustration failed. Falling back to synthetic local image. reason={}", ex.getMessage());
            } catch (IOException ex) {
                log.warn("Local memory illustration failed. Falling back to synthetic local image. reason={}", ex.getMessage());
            } catch (RuntimeException ex) {
                log.warn("Local memory illustration failed. Falling back to synthetic local image. reason={}", ex.getMessage());
            }
        }
        return fallback.generateImage(imagePrompt, diaryTitle, diaryContent);
    }

    private byte[] renderLocalMemoryIllustration(String referenceImageUrl, String imagePrompt) throws IOException, InterruptedException {
        return renderLocalMemoryIllustration(referenceImageUrl, null, imagePrompt);
    }

    private byte[] renderLocalMemoryIllustration(String referenceImageUrl, String dogReferenceImageUrl, String imagePrompt) throws IOException, InterruptedException {
        String identityImageUrl = hasText(dogReferenceImageUrl) ? dogReferenceImageUrl : referenceImageUrl;
        ImagePalette palette = promptLockedPalette(estimatePalette(identityImageUrl), imagePrompt);
        ScenePalette scene = estimateScene(referenceImageUrl);
        String prompt = imagePrompt == null ? "" : imagePrompt.toLowerCase();
        boolean pavedPrompt = prompt.contains("paved") || prompt.contains("path") || prompt.contains("walkway")
                || prompt.contains("산책로") || prompt.contains("포장") || prompt.contains("보도") || prompt.contains("길");
        boolean greenPrompt = prompt.contains("tree") || prompt.contains("grass") || prompt.contains("park")
                || prompt.contains("나무") || prompt.contains("공원") || prompt.contains("풀밭");
        boolean indoorPrompt = prompt.contains("indoor") || prompt.contains("home") || prompt.contains("cafe")
                || prompt.contains("실내") || prompt.contains("카페") || prompt.contains("집안") || prompt.contains("집에서") || prompt.contains("방 안");
        boolean indoor = scene.indoor() || indoorPrompt;
        boolean waterPrompt = prompt.contains("sea") || prompt.contains("beach") || prompt.contains("water") || prompt.contains("바다") || prompt.contains("해변") || prompt.contains("물가");
        boolean sea = (scene.water() || waterPrompt) && !pavedPrompt && !greenPrompt;
        boolean dryFieldPrompt = prompt.contains("dry grass") || prompt.contains("field") || prompt.contains("muddy")
                || prompt.contains("tall grass") || prompt.contains("마른 풀") || prompt.contains("들판") || prompt.contains("흙길") || prompt.contains("갈대");
        ScenePalette effectiveScene = new ScenePalette(
                scene.sky(),
                dryFieldPrompt ? new Color(176, 153, 102) : scene.ground(),
                scene.rawGround(),
                !dryFieldPrompt && (scene.green() || greenPrompt),
                sea,
                indoor,
                !dryFieldPrompt && (scene.paved() || pavedPrompt),
                dryFieldPrompt
        );

        BufferedImage image = new BufferedImage(1024, 768, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            drawMemoryBackground(g, sea, indoor, effectiveScene, palette);
            drawDogPointOfViewMemory(g, palette, effectiveScene);
            drawCrayonTexture(g, image.getWidth(), image.getHeight());
        } finally {
            g.dispose();
        }
        return toPng(image);
    }

    private ImagePalette promptLockedPalette(ImagePalette estimated, String imagePrompt) {
        String prompt = imagePrompt == null ? "" : imagePrompt.toLowerCase();
        boolean saysBlack = prompt.contains("black") || prompt.contains("검정") || prompt.contains("검은") || prompt.contains("검흰") || prompt.contains("보더콜리") || prompt.contains("border collie");
        boolean saysWhite = prompt.contains("white") || prompt.contains("흰") || prompt.contains("하얀") || prompt.contains("흰색") || prompt.contains("검흰") || prompt.contains("border collie");
        if (saysBlack && saysWhite) {
            return new ImagePalette(
                    true,
                    false,
                    false,
                    false,
                    new Color(250, 247, 235),
                    new Color(31, 35, 42),
                    new Color(226, 219, 206),
                    estimated.accent()
            );
        }
        if (saysWhite && !prompt.contains("brown") && !prompt.contains("tan") && !prompt.contains("갈색")) {
            return new ImagePalette(
                    false,
                    true,
                    false,
                    false,
                    new Color(250, 247, 235),
                    new Color(226, 216, 196),
                    new Color(232, 224, 210),
                    estimated.accent()
            );
        }
        return estimated;
    }

    private ImagePalette estimatePalette(String referenceImageUrl) throws IOException, InterruptedException {
        BufferedImage source = ImageIO.read(new ByteArrayInputStream(readImage(referenceImageUrl).bytes()));
        if (source == null) {
            return new ImagePalette(false, true, false, false, new Color(250, 243, 223), new Color(43, 47, 54), new Color(226, 212, 183), new Color(143, 191, 122));
        }

        int xStart = (int) Math.round(source.getWidth() * 0.30);
        int xEnd = (int) Math.round(source.getWidth() * 0.72);
        int yStart = (int) Math.round(source.getHeight() * 0.38);
        int yEnd = (int) Math.round(source.getHeight() * 0.82);
        int stepX = Math.max(1, (xEnd - xStart) / 96);
        int stepY = Math.max(1, (yEnd - yStart) / 96);
        int dark = 0;
        int light = 0;
        int brown = 0;
        int count = 0;
        long rSum = 0;
        long gSum = 0;
        long bSum = 0;

        for (int y = yStart; y < yEnd; y += stepY) {
            for (int x = xStart; x < xEnd; x += stepX) {
                Color color = new Color(source.getRGB(x, y));
                int brightness = brightness(color.getRGB());
                if (brightness < 76) {
                    dark++;
                }
                if (brightness > 200) {
                    light++;
                }
                if (color.getRed() > 88
                        && color.getRed() > color.getBlue() * 1.18
                        && color.getGreen() > color.getBlue() * 1.08
                        && brightness > 66
                        && brightness < 210) {
                    brown++;
                }
                rSum += color.getRed();
                gSum += color.getGreen();
                bSum += color.getBlue();
                count++;
            }
        }

        double darkRatio = dark / (double) count;
        double lightRatio = light / (double) count;
        double brownRatio = brown / (double) count;
        Color average = new Color((int) (rSum / count), (int) (gSum / count), (int) (bSum / count));
        boolean blackWhite = darkRatio > 0.14 && lightRatio > 0.20;
        boolean lightDog = !blackWhite && lightRatio > 0.18;
        boolean brownDog = !blackWhite && !lightDog && brownRatio > 0.18;
        boolean darkDog = !blackWhite && !brownDog && darkRatio > 0.28;

        Color body = blackWhite ? new Color(250, 246, 234)
                : lightDog ? new Color(250, 246, 234)
                : brownDog ? new Color(218, 164, 103)
                : darkDog ? new Color(58, 62, 70)
                : new Color(250, 241, 220);
        Color patch = blackWhite ? new Color(42, 46, 53)
                : lightDog ? new Color(226, 216, 196)
                : brownDog ? new Color(139, 91, 58)
                : darkDog ? new Color(246, 240, 228)
                : new Color(213, 196, 164);
        Color shadow = blackWhite ? new Color(231, 224, 210)
                : lightDog ? new Color(232, 224, 210)
                : brownDog ? new Color(189, 129, 79)
                : darkDog ? new Color(34, 38, 45)
                : new Color(226, 211, 184);

        return new ImagePalette(blackWhite, lightDog, brownDog, darkDog, body, patch, shadow, average);
    }

    private ScenePalette estimateScene(String referenceImageUrl) throws IOException, InterruptedException {
        BufferedImage source = ImageIO.read(new ByteArrayInputStream(readImage(referenceImageUrl).bytes()));
        if (source == null) {
            return new ScenePalette(new Color(204, 238, 254), new Color(145, 204, 118), new Color(150, 136, 108), true, false, false, false, false);
        }

        int width = source.getWidth();
        int height = source.getHeight();
        Color top = averageRegion(source, 0.05, 0.03, 0.95, 0.34);
        Color ground = averageRegion(source, 0.05, 0.58, 0.95, 0.95);
        int green = 0;
        int blue = 0;
        int lowerBlue = 0;
        int gray = 0;
        int warmIndoor = 0;
        int lowerCount = 0;
        int count = 0;
        int stepX = Math.max(1, width / 96);
        int stepY = Math.max(1, height / 96);

        for (int y = 0; y < height; y += stepY) {
            for (int x = 0; x < width; x += stepX) {
                Color color = new Color(source.getRGB(x, y));
                int brightness = brightness(color.getRGB());
                int spread = Math.max(color.getRed(), Math.max(color.getGreen(), color.getBlue()))
                        - Math.min(color.getRed(), Math.min(color.getGreen(), color.getBlue()));
                if (color.getGreen() > color.getRed() * 1.08 && color.getGreen() > color.getBlue() * 1.06 && brightness > 70) {
                    green++;
                }
                if (color.getBlue() > color.getRed() * 1.08 && color.getBlue() >= color.getGreen() * 0.92 && brightness > 80) {
                    blue++;
                    if (y > height * 0.42) {
                        lowerBlue++;
                    }
                }
                if (spread < 24 && brightness > 75 && brightness < 220) {
                    gray++;
                }
                if (color.getRed() > color.getBlue() * 1.18 && color.getGreen() > color.getBlue() * 1.02 && brightness > 95 && brightness < 235) {
                    warmIndoor++;
                }
                if (y > height * 0.42) {
                    lowerCount++;
                }
                count++;
            }
        }

        double greenRatio = green / (double) count;
        double blueRatio = blue / (double) count;
        double lowerBlueRatio = lowerCount == 0 ? 0 : lowerBlue / (double) lowerCount;
        double grayRatio = gray / (double) count;
        double warmRatio = warmIndoor / (double) count;
        boolean hasGreen = greenRatio > 0.10;
        boolean indoor = warmRatio > 0.38 && greenRatio < 0.08 && blueRatio < 0.15;
        boolean paved = grayRatio > 0.18 || (!hasGreen && !indoor && lowerBlueRatio < 0.22);
        boolean water = lowerBlueRatio > 0.24 && blueRatio > 0.20 && !hasGreen && !paved;
        return new ScenePalette(soften(top, new Color(206, 235, 248), 0.55), soften(ground, new Color(166, 208, 132), 0.45), ground, hasGreen, water, indoor, paved, false);
    }

    private Color averageRegion(BufferedImage source, double x1, double y1, double x2, double y2) {
        int startX = Math.max(0, (int) Math.round(source.getWidth() * x1));
        int endX = Math.min(source.getWidth(), (int) Math.round(source.getWidth() * x2));
        int startY = Math.max(0, (int) Math.round(source.getHeight() * y1));
        int endY = Math.min(source.getHeight(), (int) Math.round(source.getHeight() * y2));
        int stepX = Math.max(1, (endX - startX) / 80);
        int stepY = Math.max(1, (endY - startY) / 80);
        long r = 0;
        long g = 0;
        long b = 0;
        int count = 0;
        for (int y = startY; y < endY; y += stepY) {
            for (int x = startX; x < endX; x += stepX) {
                Color color = new Color(source.getRGB(x, y));
                r += color.getRed();
                g += color.getGreen();
                b += color.getBlue();
                count++;
            }
        }
        if (count == 0) {
            return new Color(210, 220, 210);
        }
        return new Color((int) (r / count), (int) (g / count), (int) (b / count));
    }

    private Color soften(Color color, Color fallback, double weight) {
        int r = clampToByte(color.getRed() * weight + fallback.getRed() * (1.0 - weight));
        int g = clampToByte(color.getGreen() * weight + fallback.getGreen() * (1.0 - weight));
        int b = clampToByte(color.getBlue() * weight + fallback.getBlue() * (1.0 - weight));
        return new Color(r, g, b);
    }

    private void drawMemoryBackground(Graphics2D g, boolean sea, boolean indoor, ScenePalette scene, ImagePalette palette) {
        g.setColor(indoor ? soften(scene.sky(), new Color(255, 241, 220), 0.38) : scene.sky());
        g.fillRoundRect(0, 0, 1024, 768, 42, 42);

        if (sea) {
            g.setColor(new Color(127, 200, 227, 194));
            g.fillArc(-80, 260, 1180, 210, 0, 180);
            g.setStroke(new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(255, 255, 255, 185));
            g.drawArc(70, 302, 230, 42, 180, 170);
            g.drawArc(395, 300, 270, 45, 180, 170);
            g.drawArc(760, 304, 190, 38, 180, 170);
            g.setColor(soften(scene.ground(), new Color(244, 207, 138), 0.35));
        } else if (indoor) {
            g.setColor(new Color(215, 240, 242));
            g.fillRoundRect(74, 92, 270, 176, 18, 18);
            g.setColor(new Color(145, 188, 192));
            g.setStroke(new BasicStroke(6));
            g.drawRoundRect(74, 92, 270, 176, 18, 18);
            g.setColor(soften(scene.ground(), new Color(217, 173, 114), 0.55));
        } else {
            g.setColor(new Color(255, 214, 110));
            g.fillOval(92, 68, 108, 108);
            if (scene.dryField()) {
                g.setColor(new Color(126, 101, 62));
                g.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int x = 35; x < 1010; x += 42) {
                    int h = 92 + (x % 86);
                    g.drawLine(x, 368, x + ((x % 3) - 1) * 14, Math.max(185, 368 - h));
                    g.drawLine(x + 8, 368, x + 22, Math.max(215, 368 - h + 28));
                }
                g.setColor(new Color(151, 125, 76, 120));
                g.fillOval(72, 210, 170, 90);
                g.fillOval(790, 205, 180, 96);
            } else if (scene.green()) {
                g.setColor(new Color(123, 118, 95));
                g.setStroke(new BasicStroke(11, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawLine(108, 380, 118, 210);
                g.drawLine(260, 380, 270, 225);
                g.drawLine(850, 380, 860, 212);
                g.setColor(new Color(143, 201, 129, 160));
                g.fillOval(52, 190, 120, 116);
                g.fillOval(212, 190, 135, 124);
                g.fillOval(802, 190, 132, 124);
            } else if (scene.paved()) {
                g.setColor(new Color(107, 96, 83));
                g.setStroke(new BasicStroke(10, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawLine(116, 382, 132, 230);
                g.drawLine(240, 378, 252, 214);
                g.drawLine(842, 382, 858, 214);
                g.drawLine(922, 390, 938, 246);
                g.setColor(new Color(132, 155, 138, 112));
                g.fillOval(70, 176, 112, 82);
                g.fillOval(202, 166, 126, 88);
                g.fillOval(810, 165, 126, 90);
                g.fillOval(880, 190, 92, 72);
            }
            g.setColor(scene.dryField() ? soften(scene.ground(), new Color(164, 139, 88), 0.65) : scene.green() ? soften(scene.ground(), new Color(145, 204, 118), 0.58) : soften(scene.ground(), new Color(177, 166, 145), 0.58));
        }
        g.fillRect(0, 360, 1024, 408);

        if (scene.dryField() && !sea && !indoor) {
            g.setColor(new Color(95, 75, 52, 110));
            g.setStroke(new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(438, 360, 280, 768);
            g.drawLine(586, 360, 742, 768);
            g.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int x = 20; x < 1024; x += 34) {
                int h = 65 + (x % 90);
                g.drawLine(x, 750, x + ((x % 2) == 0 ? 16 : -12), 750 - h);
            }
        }

        if (scene.paved() && !sea && !indoor) {
            g.setColor(new Color(118, 110, 96, 95));
            g.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int y = 400; y < 760; y += 58) {
                g.drawLine(70, y, 960, y - 24);
            }
            g.drawLine(315, 368, 210, 768);
            g.drawLine(710, 368, 830, 768);
        }

        g.setColor(new Color(palette.accent().getRed(), palette.accent().getGreen(), palette.accent().getBlue(), 34));
        g.setStroke(new BasicStroke(18, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawArc(58, 565, 850, 95, 182, 176);
    }

    private void drawMemoryDog(Graphics2D g, ImagePalette palette, ScenePalette scene) {
        Color outline = new Color(85, 74, 66);
        boolean subtlePatch = palette.light() && !palette.blackWhite();
        Color earColor = subtlePatch ? palette.shadow() : palette.patch();
        Color muzzleColor = palette.dark() ? new Color(246, 240, 228) : new Color(255, 250, 240);
        g.setStroke(new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        g.setColor(new Color(100, 78, 48, 38));
        g.fillOval(285, 614, 454, 70);

        g.setColor(earColor);
        g.fillRoundRect(337, 236, 92, 190, 80, 130);
        g.fillRoundRect(596, 236, 92, 190, 80, 130);
        g.setColor(outline);
        g.drawRoundRect(337, 236, 92, 190, 80, 130);
        g.drawRoundRect(596, 236, 92, 190, 80, 130);

        g.setColor(palette.body());
        g.fillOval(335, 372, 354, 290);
        drawFluffyEdge(g, palette.body(), palette.shadow(), 512, 514, 188, 132);
        g.setColor(outline);
        g.drawOval(335, 372, 354, 290);

        g.setColor(palette.shadow());
        g.fillOval(392, 512, 128, 116);
        g.fillOval(506, 512, 128, 116);
        g.setColor(outline);
        g.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawOval(392, 512, 128, 116);
        g.drawOval(506, 512, 128, 116);

        g.setStroke(new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(palette.body());
        g.fillOval(344, 208, 336, 282);
        drawFluffyEdge(g, palette.body(), palette.shadow(), 512, 350, 176, 128);
        g.setColor(outline);
        g.drawOval(344, 208, 336, 282);

        if (!subtlePatch) {
            g.setColor(palette.patch());
            g.fillArc(355, 218, 190, 170, 88, 216);
            g.fillArc(500, 214, 190, 176, 236, 216);
        } else {
            g.setColor(new Color(palette.shadow().getRed(), palette.shadow().getGreen(), palette.shadow().getBlue(), 105));
            g.fillArc(365, 232, 160, 145, 98, 180);
            g.fillArc(520, 230, 145, 142, 260, 160);
        }

        g.setColor(new Color(23, 25, 31));
        g.fillOval(438, 334, 50, 62);
        g.fillOval(536, 334, 50, 62);
        g.setColor(Color.WHITE);
        g.fillOval(458, 344, 14, 14);
        g.fillOval(556, 344, 14, 14);

        g.setColor(muzzleColor);
        g.fillOval(450, 390, 124, 94);
        g.setColor(outline);
        g.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawOval(450, 390, 124, 94);
        g.setColor(new Color(32, 33, 38));
        g.fillOval(486, 376, 62, 42);
        g.setColor(outline);
        g.drawArc(455, 405, 90, 58, 205, 125);
        g.drawArc(492, 405, 90, 58, 210, 125);

        g.setColor(new Color(247, 185, 191, 185));
        g.fillOval(384, 414, 54, 54);
        g.fillOval(586, 414, 54, 54);

        g.setColor(new Color(119, 181, 232));
        g.setStroke(new BasicStroke(18, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawArc(357, 488, 310, 78, 190, 160);
        g.setColor(new Color(255, 212, 108));
        g.fillOval(492, 530, 42, 42);
        g.setColor(new Color(173, 138, 57));
        g.setStroke(new BasicStroke(5));
        g.drawOval(492, 530, 42, 42);

    }

    private void drawDogPointOfViewMemory(Graphics2D g, ImagePalette palette, ScenePalette scene) {
        drawOwnerFromDogView(g, scene);
        drawSceneDog(g, palette);
        drawForegroundPaws(g, palette);
    }

    private void drawOwnerFromDogView(Graphics2D g, ScenePalette scene) {
        if (scene.indoor()) {
            return;
        }
        g.setColor(new Color(102, 128, 166, 170));
        g.setStroke(new BasicStroke(18, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(454, 250, 438, 356);
        g.drawLine(548, 250, 566, 356);
        g.setColor(new Color(74, 66, 62, 185));
        g.fillRoundRect(402, 342, 82, 28, 22, 18);
        g.fillRoundRect(540, 342, 82, 28, 22, 18);
        g.setColor(new Color(74, 178, 150, 150));
        g.setStroke(new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(502, 342, 490, 528);
        g.setColor(new Color(255, 255, 255, 92));
        g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(462, 260, 448, 340);
        g.drawLine(556, 260, 570, 340);
    }

    private void drawSceneDog(Graphics2D g, ImagePalette palette) {
        Color outline = new Color(91, 77, 66);
        Color muzzle = palette.dark() ? new Color(246, 240, 228) : new Color(255, 250, 240);
        boolean subtlePatch = palette.light() && !palette.blackWhite();
        Color ear = subtlePatch ? palette.shadow() : palette.patch();

        g.setColor(new Color(95, 76, 56, 34));
        g.fillOval(352, 552, 314, 62);

        g.setStroke(new BasicStroke(7, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(palette.body());
        g.fillRoundRect(382, 400, 244, 174, 120, 120);
        drawFluffyEdge(g, palette.body(), palette.shadow(), 505, 486, 132, 82);
        g.setColor(outline);
        g.drawRoundRect(382, 400, 244, 174, 120, 120);

        g.setColor(ear);
        g.fillOval(372, 322, 74, 126);
        g.fillOval(566, 322, 74, 126);
        g.setColor(outline);
        g.drawOval(372, 322, 74, 126);
        g.drawOval(566, 322, 74, 126);

        g.setColor(palette.body());
        g.fillOval(390, 304, 230, 188);
        drawFluffyEdge(g, palette.body(), palette.shadow(), 505, 398, 122, 86);
        g.setColor(outline);
        g.drawOval(390, 304, 230, 188);

        if (!subtlePatch) {
            g.setColor(palette.patch());
            g.fillArc(404, 322, 104, 88, 86, 210);
            g.fillArc(506, 320, 104, 88, 240, 210);
        } else {
            g.setColor(new Color(palette.shadow().getRed(), palette.shadow().getGreen(), palette.shadow().getBlue(), 88));
            g.fillArc(410, 328, 90, 76, 96, 160);
            g.fillArc(524, 328, 84, 76, 282, 150);
        }

        g.setColor(new Color(22, 24, 30));
        g.fillOval(450, 376, 34, 42);
        g.fillOval(532, 376, 34, 42);
        g.setColor(Color.WHITE);
        g.fillOval(462, 384, 9, 9);
        g.fillOval(544, 384, 9, 9);

        g.setColor(muzzle);
        g.fillOval(462, 420, 90, 66);
        g.setColor(outline);
        g.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawOval(462, 420, 90, 66);
        g.setColor(new Color(31, 32, 38));
        g.fillOval(488, 408, 44, 28);
        g.setColor(outline);
        g.drawArc(468, 428, 58, 38, 205, 120);
        g.drawArc(498, 428, 58, 38, 210, 120);

        g.setColor(new Color(247, 185, 191, 160));
        g.fillOval(418, 430, 38, 38);
        g.fillOval(562, 430, 38, 38);

        g.setColor(new Color(113, 188, 156));
        g.setStroke(new BasicStroke(16, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawArc(408, 476, 204, 58, 190, 160);
        g.setColor(new Color(255, 214, 108));
        g.fillOval(488, 506, 34, 34);
        g.setColor(new Color(173, 138, 57));
        g.setStroke(new BasicStroke(4));
        g.drawOval(488, 506, 34, 34);
    }

    private void drawForegroundPaws(Graphics2D g, ImagePalette palette) {
        Color outline = new Color(91, 77, 66);
        g.setColor(new Color(255, 255, 255, 115));
        g.fillOval(102, 644, 244, 92);
        g.fillOval(678, 644, 244, 92);
        g.setColor(palette.body());
        g.fillRoundRect(134, 602, 180, 118, 80, 80);
        g.fillRoundRect(710, 602, 180, 118, 80, 80);
        g.setColor(outline);
        g.setStroke(new BasicStroke(7, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawRoundRect(134, 602, 180, 118, 80, 80);
        g.drawRoundRect(710, 602, 180, 118, 80, 80);
        g.setColor(new Color(palette.shadow().getRed(), palette.shadow().getGreen(), palette.shadow().getBlue(), 180));
        g.fillOval(178, 636, 26, 30);
        g.fillOval(218, 628, 28, 32);
        g.fillOval(258, 638, 26, 30);
        g.fillOval(754, 638, 26, 30);
        g.fillOval(794, 628, 28, 32);
        g.fillOval(834, 636, 26, 30);
        g.setColor(new Color(74, 178, 150, 145));
        g.setStroke(new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(490, 528, 300, 768);
    }

    private void drawMemoryLeash(Graphics2D g, ScenePalette scene) {
        if ((scene.paved() || scene.green()) && !scene.indoor()) {
            g.setColor(new Color(74, 178, 150, 150));
            g.setStroke(new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(510, 552, 300, 768);
            g.setColor(new Color(74, 178, 150, 58));
            g.setStroke(new BasicStroke(14, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(510, 552, 300, 768);
        }
    }

    private void drawFluffyEdge(Graphics2D g, Color body, Color shadow, int centerX, int centerY, int radiusX, int radiusY) {
        g.setColor(body);
        for (int i = 0; i < 20; i++) {
            double angle = Math.toRadians(i * 18.0);
            int x = centerX + (int) Math.round(Math.cos(angle) * radiusX) - 25;
            int y = centerY + (int) Math.round(Math.sin(angle) * radiusY) - 22;
            int size = i % 2 == 0 ? 54 : 44;
            g.fillOval(x, y, size, size);
        }
        g.setColor(new Color(shadow.getRed(), shadow.getGreen(), shadow.getBlue(), 76));
        for (int i = 1; i < 20; i += 3) {
            double angle = Math.toRadians(i * 18.0);
            int x = centerX + (int) Math.round(Math.cos(angle) * radiusX) - 16;
            int y = centerY + (int) Math.round(Math.sin(angle) * radiusY) - 13;
            g.fillOval(x, y, 28, 28);
        }
    }

    private void drawCrayonTexture(Graphics2D g, int width, int height) {
        g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int y = 8; y < height; y += 12) {
            g.setColor(new Color(255, 255, 255, 18));
            g.drawLine(0, y, width, Math.max(0, y - 22));
        }
        g.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 160; i++) {
            int x = (i * 73) % width;
            int y = (i * 41) % height;
            g.setColor(new Color(90, 70, 55, 14));
            g.drawLine(x, y, Math.min(width, x + 26), Math.max(0, y - 5 + (i % 11)));
        }
    }

    private byte[] stylizeReferenceImage(String referenceImageUrl) throws IOException, InterruptedException {
        BufferedImage source = ImageIO.read(new ByteArrayInputStream(readImage(referenceImageUrl).bytes()));
        if (source == null) {
            return new byte[0];
        }

        int targetWidth = Math.min(1024, Math.max(640, source.getWidth()));
        int targetHeight = Math.max(480, (int) Math.round(source.getHeight() * (targetWidth / (double) source.getWidth())));
        BufferedImage scaled = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        } finally {
            g.dispose();
        }

        BufferedImage output = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < targetHeight; y++) {
            for (int x = 0; x < targetWidth; x++) {
                Color color = new Color(scaled.getRGB(x, y));
                int edge = edgeStrength(scaled, x, y);
                int noise = ((x * 17 + y * 31) % 11) - 5;
                int r = crayonChannel(color.getRed(), edge, noise);
                int green = crayonChannel(color.getGreen(), edge, noise);
                int b = crayonChannel(color.getBlue(), edge, noise);
                output.setRGB(x, y, new Color(r, green, b).getRGB());
            }
        }

        Graphics2D overlay = output.createGraphics();
        try {
            overlay.setStroke(new BasicStroke(1.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int y = 5; y < targetHeight; y += 11) {
                overlay.setColor(new Color(255, 255, 255, 16));
                overlay.drawLine(0, y, targetWidth, Math.max(0, y - 18));
            }
            overlay.setColor(new Color(70, 58, 54, 16));
            overlay.setStroke(new BasicStroke(0.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int y = 1; y < targetHeight - 1; y += 3) {
                for (int x = 1; x < targetWidth - 1; x += 3) {
                    if (edgeStrength(scaled, x, y) > 78 && ((x + y) % 11 == 0)) {
                        overlay.drawLine(x - 1, y, x + 1, y + ((x + y) % 3) - 1);
                    }
                }
            }
        } finally {
            overlay.dispose();
        }

        return toPng(output);
    }

    private int edgeStrength(BufferedImage image, int x, int y) {
        int left = brightness(image.getRGB(Math.max(0, x - 1), y));
        int right = brightness(image.getRGB(Math.min(image.getWidth() - 1, x + 1), y));
        int top = brightness(image.getRGB(x, Math.max(0, y - 1)));
        int bottom = brightness(image.getRGB(x, Math.min(image.getHeight() - 1, y + 1)));
        return Math.min(255, Math.abs(left - right) + Math.abs(top - bottom));
    }

    private int brightness(int rgb) {
        Color color = new Color(rgb);
        return (int) (color.getRed() * 0.299 + color.getGreen() * 0.587 + color.getBlue() * 0.114);
    }

    private int crayonChannel(int value, int edge, int noise) {
        int softened = (int) Math.round(value * 0.66 + 255 * 0.34);
        int posterized = Math.round(softened / 4.0f) * 4;
        int edged = posterized - Math.min(22, edge / 7) + noise;
        return Math.max(0, Math.min(255, edged));
    }

    private int clampToByte(double value) {
        return Math.max(0, Math.min(255, (int) Math.round(value)));
    }

    private String firstImageUrl(JsonNode root) {
        JsonNode images = root.path("images");
        if (images.isArray() && !images.isEmpty()) {
            JsonNode first = images.get(0);
            if (first.isTextual()) {
                return first.asText();
            }
            String url = first.path("url").asText();
            if (!url.isBlank()) {
                return url;
            }
        }

        JsonNode data = root.path("data");
        if (data.isArray() && !data.isEmpty()) {
            String url = data.get(0).path("url").asText();
            if (!url.isBlank()) {
                return url;
            }
        }

        return root.path("url").asText();
    }

    private String stripDataUrlPrefix(String value) {
        int commaIndex = value.indexOf(',');
        return value.startsWith("data:") && commaIndex > 0 ? value.substring(commaIndex + 1) : value;
    }

    private String safeBody(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        String text = new String(bytes, 0, Math.min(bytes.length, 360));
        return text.replaceAll("[\\r\\n]+", " ");
    }

    private byte[] composeDiaryPage(byte[] drawingBytes, String diaryTitle, String diaryContent) throws IOException {
        BufferedImage drawing = ImageIO.read(new ByteArrayInputStream(drawingBytes));
        if (drawing == null) {
            return drawingBytes;
        }

        BufferedImage page = new BufferedImage(PAGE_WIDTH, PAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = page.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            drawPaper(g);
            drawHeader(g, diaryTitle);
            drawUpperDrawing(g, drawing);
            drawDiaryText(g, diaryContent);
        } finally {
            g.dispose();
        }

        return toPng(page);
    }

    private void drawPaper(Graphics2D g) {
        g.setColor(new Color(255, 253, 246));
        g.fillRect(0, 0, PAGE_WIDTH, PAGE_HEIGHT);

        g.setColor(new Color(222, 222, 214));
        g.setStroke(new BasicStroke(2));
        g.drawRect(28, 28, PAGE_WIDTH - 56, PAGE_HEIGHT - 56);

        g.setColor(new Color(230, 230, 224));
        for (int y = 78; y < PAGE_HEIGHT - 40; y += LINE_HEIGHT) {
            g.drawLine(40, y, PAGE_WIDTH - 40, y);
        }

        g.setColor(new Color(190, 218, 229));
        g.setStroke(new BasicStroke(2));
        g.drawLine(105, 42, 105, PAGE_HEIGHT - 42);

        g.setColor(new Color(116, 116, 110));
        g.fillOval(62, 248, 20, 20);
        g.fillOval(62, 510, 20, 20);
        g.fillOval(62, 736, 20, 20);
    }

    private void drawHeader(Graphics2D g, String diaryTitle) {
        Font labelFont = new Font("Malgun Gothic", Font.PLAIN, 22);
        Font textFont = new Font("Malgun Gothic", Font.BOLD, 24);
        g.setFont(labelFont);
        g.setColor(new Color(58, 58, 54));

        int left = 124;
        int right = 470;
        g.drawString("이름: 보리", left, 72);
        g.drawString("날짜: " + LocalDate.now(), left, 114);
        g.drawString("제목:", right, 72);
        g.drawString("날씨: 맑음", right, 114);

        g.setFont(textFont);
        g.drawString(shorten(diaryTitle, 14), right + 68, 72);
    }

    private void drawUpperDrawing(Graphics2D g, BufferedImage drawing) {
        int x = MARGIN;
        int y = DRAWING_TOP;
        int w = PAGE_WIDTH - MARGIN * 2;
        int h = DRAWING_HEIGHT;

        g.setColor(Color.WHITE);
        g.fillRect(x, y, w, h);
        g.setColor(new Color(72, 72, 68));
        g.setStroke(new BasicStroke(3));
        g.drawRect(x, y, w, h);

        int sourceTop = Math.max(0, (int) (drawing.getHeight() * 0.08));
        int sourceBottom = Math.max(sourceTop + 1, (int) (drawing.getHeight() * 0.82));
        int sourceHeight = sourceBottom - sourceTop;
        double scale = Math.min((double) (w - 28) / drawing.getWidth(), (double) (h - 28) / sourceHeight);
        int drawW = (int) (drawing.getWidth() * scale);
        int drawH = (int) (sourceHeight * scale);
        int drawX = x + (w - drawW) / 2;
        int drawY = y + (h - drawH) / 2;
        g.drawImage(drawing, drawX, drawY, drawX + drawW, drawY + drawH,
                0, sourceTop, drawing.getWidth(), sourceBottom, null);
    }

    private void drawDiaryText(Graphics2D g, String diaryContent) {
        Font textFont = new Font("Malgun Gothic", Font.PLAIN, 29);
        g.setFont(textFont);
        g.setColor(new Color(70, 66, 62));

        int x = MARGIN + 12;
        int y = TEXT_TOP + 30;
        int maxWidth = PAGE_WIDTH - MARGIN * 2 - 24;
        List<String> lines = wrapLines(g.getFontMetrics(), diaryContent, maxWidth);

        for (int i = 0; i < Math.min(lines.size(), 10); i++) {
            String line = lines.get(i);
            drawChildlikeLine(g, line, x, y + i * LINE_HEIGHT, i);
        }
    }

    private void drawChildlikeLine(Graphics2D g, String line, int x, int y, int lineIndex) {
        FontMetrics metrics = g.getFontMetrics();
        int cursor = x + switch (lineIndex % 4) {
            case 1 -> 4;
            case 2 -> -3;
            case 3 -> 2;
            default -> 0;
        };

        for (int i = 0; i < line.length(); i++) {
            String ch = line.substring(i, i + 1);
            int baselineJitter = ((i + lineIndex) % 5) - 2;
            double angle = Math.toRadians((((i * 7) + lineIndex) % 7 - 3) * 0.9);

            g.rotate(angle, cursor, y + baselineJitter);
            g.drawString(ch, cursor, y + baselineJitter);
            g.rotate(-angle, cursor, y + baselineJitter);
            cursor += metrics.stringWidth(ch) + ((i + lineIndex) % 3 == 0 ? 1 : 0);
        }
    }

    private List<String> wrapLines(FontMetrics metrics, String text, int maxWidth) {
        List<String> result = new ArrayList<>();
        for (String paragraph : text.split("\\R")) {
            StringBuilder line = new StringBuilder();
            for (int i = 0; i < paragraph.length(); i++) {
                char ch = paragraph.charAt(i);
                String candidate = line + String.valueOf(ch);
                if (metrics.stringWidth(candidate) > maxWidth && !line.isEmpty()) {
                    result.add(line.toString());
                    line = new StringBuilder(String.valueOf(ch));
                } else {
                    line.append(ch);
                }
            }
            if (!line.isEmpty()) {
                result.add(line.toString());
            }
        }
        return result;
    }

    private byte[] toPng(BufferedImage page) throws IOException {
        Path temp = Files.createTempFile("diary-page", ".png");
        try {
            ImageIO.write(page, "png", temp.toFile());
            return Files.readAllBytes(temp);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private boolean looksLikeJson(byte[] bytes) {
        for (byte b : bytes) {
            if (!Character.isWhitespace((char) b)) {
                return b == '{' || b == '[';
            }
        }
        return false;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String saveImage(byte[] imageBytes) throws IOException {
        Path uploadDirectory = Path.of("uploads", "generated");
        Files.createDirectories(uploadDirectory);

        String filename = "diary-" + Instant.now().toEpochMilli() + ".png";
        Path outputPath = uploadDirectory.resolve(filename);
        Files.write(outputPath, imageBytes);

        return "/uploads/generated/" + filename;
    }

    private String shorten(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "오늘의 산책";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private record ImagePayload(byte[] bytes, String mimeType) {
    }

    private record ImagePalette(
            boolean blackWhite,
            boolean light,
            boolean brown,
            boolean dark,
            Color body,
            Color patch,
            Color shadow,
            Color accent
    ) {
    }

    private record ScenePalette(
            Color sky,
            Color ground,
            Color rawGround,
            boolean green,
            boolean water,
            boolean indoor,
            boolean paved,
            boolean dryField
    ) {
    }
}
