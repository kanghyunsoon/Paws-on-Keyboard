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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Primary
public class CloudflareAiImageService implements AiImageService {

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

    public CloudflareAiImageService(
            FakeAiImageService fallback,
            @Value("${app.ai.cloudflare-account-id:}") String accountId,
            @Value("${app.ai.cloudflare-api-token:}") String apiToken,
            @Value("${app.ai.cloudflare-image-model:@cf/black-forest-labs/flux-1-schnell}") String model
    ) {
        this.restClient = RestClient.create();
        this.objectMapper = new ObjectMapper();
        this.fallback = fallback;
        this.accountId = accountId;
        this.apiToken = apiToken;
        this.model = model;
    }

    @Override
    public String generateImage(String imagePrompt, String diaryTitle, String diaryContent) {
        if (accountId == null || accountId.isBlank() || apiToken == null || apiToken.isBlank()) {
            return fallback.generateImage(imagePrompt, diaryTitle, diaryContent);
        }

        try {
            byte[] responseBytes = restClient.post()
                    .uri("https://api.cloudflare.com/client/v4/accounts/" + accountId + "/ai/run/" + model)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
                    .body(Map.of("prompt", imagePrompt))
                    .retrieve()
                    .body(byte[].class);

            byte[] drawingBytes = extractImageBytes(responseBytes);
            if (drawingBytes.length == 0) {
                return fallback.generateImage(imagePrompt, diaryTitle, diaryContent);
            }

            byte[] diaryPage = composeDiaryPage(drawingBytes, diaryTitle, diaryContent);
            return saveImage(diaryPage);
        } catch (RuntimeException | IOException ex) {
            return fallback.generateImage(imagePrompt, diaryTitle, diaryContent);
        }
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
            if (base64Image.isBlank()) {
                return new byte[0];
            }
            return Base64.getDecoder().decode(base64Image);
        }

        return responseBytes;
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
                String candidate = line.toString() + ch;
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
}
