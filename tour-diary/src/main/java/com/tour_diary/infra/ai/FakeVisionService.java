package com.tour_diary.infra.ai;

import com.tour_diary.ai.vision.VisionAnalysisResult;
import com.tour_diary.ai.vision.VisionService;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;

@Service
public class FakeVisionService implements VisionService {

    @Override
    public VisionAnalysisResult analyze(String imageUrl) {
        try {
            BufferedImage image = readImage(imageUrl);
            if (image != null) {
                return analyzeLocally(image);
            }
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }

        return new VisionAnalysisResult(
                true,
                List.of("업로드 사진 속 강아지", "산책 배경", "보호자와 이어지는 단서"),
                List.of("사진에서 보이는 실제 털색", "사진 배경의 실제 색감"),
                "사진에서 보이는 실제 산책 순간",
                "사진 속 산책 장소",
                List.of("발자국", "리드줄", "코끝 냄새"),
                List.of("사진 속 자세를 기준으로 기억하기", "보호자를 올려다보는 마음"),
                "강아지가 보호자와 함께 사진 속 장소에 머문 순간",
                "초기 설정과 사진에서 보이는 털색, 무늬, 귀, 표정을 우선",
                "보호자가 보이면 위치 단서만 사용하고, 보이지 않으면 지어내지 않음",
                "사진 속 자세를 바탕으로 산책 중인 모습",
                "강아지가 낮은 시선에서 냄새와 발바닥 느낌으로 기억한 장면",
                List.of("사진 속 강아지", "실제 배경", "낮은 강아지 시점", "귀여운 삐뚤빼뚤 그림")
        );
    }

    private BufferedImage readImage(String imageUrl) throws IOException, InterruptedException {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }
        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            HttpResponse<java.io.InputStream> response = HttpClient.newHttpClient()
                    .send(HttpRequest.newBuilder(URI.create(imageUrl)).GET().build(), HttpResponse.BodyHandlers.ofInputStream());
            return ImageIO.read(response.body());
        }
        Path path = imageUrl.startsWith("/")
                ? Path.of("." + imageUrl).normalize()
                : Path.of(imageUrl).normalize();
        if (!Files.exists(path)) {
            return null;
        }
        return ImageIO.read(path.toFile());
    }

    private VisionAnalysisResult analyzeLocally(BufferedImage image) {
        PhotoStats full = stats(image, 0.04, 0.04, 0.96, 0.96);
        PhotoStats dogArea = stats(image, 0.28, 0.32, 0.76, 0.88);
        PhotoStats lower = stats(image, 0.05, 0.55, 0.95, 0.96);

        boolean blackWhiteDog = dogArea.darkRatio() > 0.14 && dogArea.lightRatio() > 0.18;
        boolean whiteDog = !blackWhiteDog && dogArea.lightRatio() > 0.16;
        boolean brownDog = !blackWhiteDog && !whiteDog && dogArea.brownRatio() > 0.16;
        boolean darkDog = !blackWhiteDog && !brownDog && dogArea.darkRatio() > 0.26;

        boolean greenScene = full.greenRatio() > 0.10;
        boolean pavedScene = lower.grayRatio() > 0.18 || (!greenScene && lower.blueRatio() < 0.18);
        boolean waterScene = lower.blueRatio() > 0.24 && !pavedScene;
        boolean indoorScene = full.warmRatio() > 0.38 && full.greenRatio() < 0.08 && full.blueRatio() < 0.16;
        boolean leashLike = full.tealRatio() > 0.012 || full.redRatio() > 0.018;

        String fur = blackWhiteDog ? "검정과 흰색이 섞인 털"
                : whiteDog ? "밝고 하얀 계열의 털"
                : brownDog ? "갈색 계열의 털"
                : darkDog ? "어두운 계열의 털"
                : "사진에서 보이는 자연스러운 털색";
        String place = indoorScene ? "실내 공간"
                : waterScene ? "물가 산책 장소"
                : pavedScene ? "포장된 산책길"
                : greenScene ? "초록빛 야외 산책 장소"
                : "사진 속 산책 장소";
        String ground = pavedScene ? "보도블록이나 포장길"
                : greenScene ? "풀밭과 야외 바닥"
                : waterScene ? "물가와 밝은 바닥"
                : indoorScene ? "실내 바닥"
                : "사진 속 바닥";

        List<String> objects = new ArrayList<>();
        objects.add("사진 속 강아지");
        objects.add(ground);
        if (greenScene || pavedScene) {
            objects.add("나무나 산책길 배경");
        }
        if (leashLike) {
            objects.add("목줄이나 리드줄 단서");
        }
        if (waterScene) {
            objects.add("물가 배경");
        }
        if (indoorScene) {
            objects.add("실내의 따뜻한 배경");
        }

        List<String> colors = new ArrayList<>();
        colors.add(fur);
        colors.add(pavedScene ? "회색빛 산책길" : greenScene ? "초록빛 배경" : waterScene ? "푸른빛 배경" : "사진 배경 색감");
        colors.add("사진에서 추출한 부드러운 실제 색감");

        List<String> drawingKeywords = new ArrayList<>();
        drawingKeywords.add(fur + " 강아지");
        drawingKeywords.add(place);
        drawingKeywords.add(ground);
        drawingKeywords.add("강아지 중심 구도");
        drawingKeywords.add("낮은 강아지 시점");
        drawingKeywords.add("삐뚤빼뚤하지만 예쁜 파스텔 색연필 그림");
        if (leashLike) {
            drawingKeywords.add("리드줄");
        }
        if (greenScene || pavedScene) {
            drawingKeywords.add("나무가 보이는 산책길");
        }

        return new VisionAnalysisResult(
                true,
                objects,
                colors,
                place + "에서 보이는 실제 산책 순간",
                place,
                List.of("발자국", leashLike ? "리드줄" : "코끝 냄새", "사진 속 배경 색감"),
                List.of("사진 속 바닥을 밟는 느낌", "보호자와 이어진 산책 기억", fur + "을 유지한 귀여운 그림"),
                place + "에서 " + fur + " 강아지가 산책 중인 사진",
                fur + ", 사진에서 보이는 무늬와 귀, 얼굴, 목줄 단서를 우선",
                leashLike ? "리드줄로 보호자와 이어져 있음. 보호자 성별은 추정하지 않음" : "보호자는 사진에서 명확히 보이지 않음",
                "사진 속 자리에서 주변을 살피거나 산책 중인 모습",
                "강아지가 낮은 위치에서 바닥 냄새와 보호자 쪽 리드줄을 기억하는 시점",
                drawingKeywords
        );
    }

    private PhotoStats stats(BufferedImage image, double x1, double y1, double x2, double y2) {
        int startX = Math.max(0, (int) Math.round(image.getWidth() * x1));
        int endX = Math.min(image.getWidth(), (int) Math.round(image.getWidth() * x2));
        int startY = Math.max(0, (int) Math.round(image.getHeight() * y1));
        int endY = Math.min(image.getHeight(), (int) Math.round(image.getHeight() * y2));
        int stepX = Math.max(1, (endX - startX) / 96);
        int stepY = Math.max(1, (endY - startY) / 96);

        int dark = 0;
        int light = 0;
        int brown = 0;
        int green = 0;
        int blue = 0;
        int gray = 0;
        int warm = 0;
        int teal = 0;
        int red = 0;
        int count = 0;

        for (int y = startY; y < endY; y += stepY) {
            for (int x = startX; x < endX; x += stepX) {
                Color color = new Color(image.getRGB(x, y));
                int brightness = brightness(color);
                int spread = Math.max(color.getRed(), Math.max(color.getGreen(), color.getBlue()))
                        - Math.min(color.getRed(), Math.min(color.getGreen(), color.getBlue()));
                if (brightness < 76) dark++;
                if (brightness > 200) light++;
                if (color.getRed() > 88 && color.getRed() > color.getBlue() * 1.18 && color.getGreen() > color.getBlue() * 1.06 && brightness > 66 && brightness < 210) brown++;
                if (color.getGreen() > color.getRed() * 1.08 && color.getGreen() > color.getBlue() * 1.06 && brightness > 70) green++;
                if (color.getBlue() > color.getRed() * 1.08 && color.getBlue() >= color.getGreen() * 0.92 && brightness > 80) blue++;
                if (spread < 24 && brightness > 75 && brightness < 220) gray++;
                if (color.getRed() > color.getBlue() * 1.18 && color.getGreen() > color.getBlue() * 1.02 && brightness > 95 && brightness < 235) warm++;
                if (color.getGreen() > color.getRed() * 1.12 && color.getBlue() > color.getRed() * 1.10 && brightness > 80) teal++;
                if (color.getRed() > color.getGreen() * 1.18 && color.getRed() > color.getBlue() * 1.20 && brightness > 70) red++;
                count++;
            }
        }

        int safeCount = Math.max(1, count);
        return new PhotoStats(
                dark / (double) safeCount,
                light / (double) safeCount,
                brown / (double) safeCount,
                green / (double) safeCount,
                blue / (double) safeCount,
                gray / (double) safeCount,
                warm / (double) safeCount,
                teal / (double) safeCount,
                red / (double) safeCount
        );
    }

    private int brightness(Color color) {
        return (int) (color.getRed() * 0.299 + color.getGreen() * 0.587 + color.getBlue() * 0.114);
    }

    private record PhotoStats(
            double darkRatio,
            double lightRatio,
            double brownRatio,
            double greenRatio,
            double blueRatio,
            double grayRatio,
            double warmRatio,
            double tealRatio,
            double redRatio
    ) {
    }
}
