package com.tour_diary.infra.ai;

import com.tour_diary.ai.image.AiImageService;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;

@Service
public class FakeAiImageService implements AiImageService {

    @Override
    public String generateImage(String imagePrompt, String diaryTitle, String diaryContent) {
        try {
            Path uploadDirectory = Path.of("uploads", "generated");
            Files.createDirectories(uploadDirectory);
            String filename = "diary-preview-" + Instant.now().toEpochMilli() + ".png";
            Path outputPath = uploadDirectory.resolve(filename);
            Files.write(outputPath, renderPreview());
            return "/uploads/generated/" + filename;
        } catch (IOException ex) {
            return "";
        }
    }

    private byte[] renderPreview() throws IOException {
        BufferedImage image = new BufferedImage(1024, 768, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(210, 235, 247));
            g.fillRect(0, 0, 1024, 360);
            g.setColor(new Color(176, 202, 145));
            g.fillRect(0, 360, 1024, 408);
            g.setColor(new Color(139, 118, 92));
            g.setStroke(new BasicStroke(10, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(180, 360, 190, 180);
            g.drawLine(820, 360, 830, 185);
            g.setColor(new Color(150, 181, 128));
            g.fillOval(120, 130, 140, 110);
            g.fillOval(760, 130, 140, 110);
            g.setColor(new Color(255, 252, 240));
            g.fillRoundRect(360, 300, 300, 230, 120, 120);
            g.setColor(new Color(74, 66, 58));
            g.setStroke(new BasicStroke(9, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawRoundRect(360, 300, 300, 230, 120, 120);
            g.setColor(new Color(31, 34, 40));
            g.fillOval(422, 352, 72, 82);
            g.fillOval(532, 352, 72, 82);
            g.setColor(Color.WHITE);
            g.fillOval(450, 372, 14, 14);
            g.fillOval(560, 372, 14, 14);
            g.setColor(new Color(35, 36, 40));
            g.fillOval(480, 430, 66, 44);
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
