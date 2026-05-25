package com.tour_diary.upload.controller;

import com.tour_diary.upload.controller.dto.UploadImageResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Locale;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(value = "/api/uploads", produces = "application/json; charset=UTF-8")
public class UploadController {

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadImageResponse uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "walk") String type
    ) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 이미지가 비어 있습니다.");
        }

        String extension = extensionOf(file.getOriginalFilename());
        String safeType = type.replaceAll("[^a-zA-Z0-9_-]", "").toLowerCase(Locale.ROOT);
        String filename = safeType + "-" + Instant.now().toEpochMilli() + extension;
        Path directory = Path.of("uploads", "images").toAbsolutePath().normalize();
        Files.createDirectories(directory);
        Path target = directory.resolve(filename).normalize();
        file.transferTo(target);
        return new UploadImageResponse(filename, "/uploads/images/" + filename);
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
        return extension.matches("\\.(jpg|jpeg|png|webp|gif)") ? extension : ".jpg";
    }
}
