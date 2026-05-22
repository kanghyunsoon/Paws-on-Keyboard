package com.tour_diary.infra.config;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> values = loadLocalEnv();
        if (!values.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource("localDotenv", values));
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private Map<String, Object> loadLocalEnv() {
        Map<String, Object> values = new HashMap<>();
        Optional<Path> envPath = findEnvPath();
        if (envPath.isEmpty()) {
            return values;
        }

        try {
            for (String line : Files.readAllLines(envPath.get())) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) {
                    continue;
                }

                int separator = trimmed.indexOf('=');
                String key = trimmed.substring(0, separator).trim();
                String value = stripWrappingQuotes(trimmed.substring(separator + 1).trim());
                if (!key.isEmpty()) {
                    values.put(key, value);
                    mappedSpringProperty(key, value, values);
                }
            }
        } catch (IOException ignored) {
            // The app can still run with normal environment variables.
        }

        return values;
    }

    private Optional<Path> findEnvPath() {
        Path currentDirectory = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        Path classDirectory = classDirectory();
        Path[] candidates = {
                currentDirectory.resolve(".env"),
                currentDirectory.resolve("tour-diary").resolve(".env"),
                classDirectory.resolve(".env"),
                classDirectory.resolve("../../../../.env").normalize()
        };

        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private Path classDirectory() {
        try {
            return Path.of(DotenvEnvironmentPostProcessor.class.getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI())
                    .toAbsolutePath();
        } catch (URISyntaxException ex) {
            return Path.of(System.getProperty("user.dir")).toAbsolutePath();
        }
    }

    private void mappedSpringProperty(String key, String value, Map<String, Object> values) {
        switch (key) {
            case "MONGODB_URI" -> {
                values.put("spring.mongodb.uri", value);
                values.put("spring.data.mongodb.uri", value);
            }
            case "MONGODB_DATABASE" -> {
                values.put("spring.mongodb.database", value);
                values.put("spring.data.mongodb.database", value);
            }
            case "GEMINI_API_KEY" -> values.put("app.ai.gemini-api-key", value);
            case "GEMINI_MODEL" -> values.put("app.ai.gemini-model", value);
            case "GROQ_API_KEY" -> values.put("app.ai.groq-api-key", value);
            case "GROQ_MODEL" -> values.put("app.ai.groq-model", value);
            case "CLOUDFLARE_ACCOUNT_ID" -> values.put("app.ai.cloudflare-account-id", value);
            case "CLOUDFLARE_API_TOKEN" -> values.put("app.ai.cloudflare-api-token", value);
            case "CLOUDFLARE_IMAGE_MODEL" -> values.put("app.ai.cloudflare-image-model", value);
            case "HUGGINGFACE_TOKEN" -> values.put("app.ai.huggingface-token", value);
            case "AI_IMAGE_GENERATE_URL" -> values.put("app.ai.image-generate-url", value);
            case "KAKAO_REST_API_KEY" -> values.put("app.kakao.rest-api-key", value);
            case "KTO_TOUR_API_KEY" -> values.put("app.kto.tour-api-key", value);
            case "KTO_TOUR_API_BASE_URL" -> values.put("app.kto.base-url", value);
            case "KTO_PET_TOUR_API_KEY" -> values.put("app.kto.pet-tour-api-key", value);
            case "KTO_PET_TOUR_API_BASE_URL" -> values.put("app.kto.pet-tour-base-url", value);
            case "KTO_DURUNUBI_API_KEY" -> values.put("app.kto.durunubi-api-key", value);
            case "KTO_ACCESSIBLE_TOUR_API_KEY" -> values.put("app.kto.accessible-tour-api-key", value);
            case "KTO_GREEN_TOUR_API_KEY" -> values.put("app.kto.green-tour-api-key", value);
            default -> {
            }
        }
    }

    private String stripWrappingQuotes(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
