package com.tour_diary.infra.weather;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tour_diary.weather.WeatherResolver;
import com.tour_diary.weather.WeatherSummary;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Primary
public class KmaWeatherResolver implements WeatherResolver {

    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH00");

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String baseUrl;
    private final boolean externalApiEnabled;

    public KmaWeatherResolver(
            @Value("${app.weather.kma-api-key:}") String apiKey,
            @Value("${app.weather.kma-base-url:https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0}") String baseUrl,
            @Value("${app.external-api-enabled:true}") boolean externalApiEnabled
    ) {
        this.restClient = RestClient.create();
        this.objectMapper = new ObjectMapper();
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.externalApiEnabled = externalApiEnabled;
    }

    @Override
    public Optional<WeatherSummary> resolve(Double latitude, Double longitude) {
        if (!externalApiEnabled || apiKey == null || apiKey.isBlank() || latitude == null || longitude == null) {
            return Optional.empty();
        }

        try {
            GridPoint grid = toGrid(latitude, longitude);
            LocalDateTime base = LocalDateTime.now(KOREA).minusMinutes(45);
            URI uri = URI.create(baseUrl + "/getUltraSrtNcst"
                    + "?serviceKey=" + encode(apiKey)
                    + "&pageNo=1"
                    + "&numOfRows=20"
                    + "&dataType=JSON"
                    + "&base_date=" + base.format(DATE_FORMAT)
                    + "&base_time=" + base.format(TIME_FORMAT)
                    + "&nx=" + grid.x()
                    + "&ny=" + grid.y());

            String response = restClient.get().uri(uri).retrieve().body(String.class);
            Map<String, String> values = parseItems(response);
            String description = buildDescription(values);
            return description.isBlank() ? Optional.empty() : Optional.of(new WeatherSummary(description, "기상청 초단기실황"));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private Map<String, String> parseItems(String response) throws Exception {
        Map<String, String> values = new HashMap<>();
        JsonNode items = objectMapper.readTree(response)
                .path("response")
                .path("body")
                .path("items")
                .path("item");
        if (items.isObject()) {
            values.put(items.path("category").asText(), items.path("obsrValue").asText());
            return values;
        }
        if (items.isArray()) {
            for (JsonNode item : items) {
                values.put(item.path("category").asText(), item.path("obsrValue").asText());
            }
        }
        return values;
    }

    private String buildDescription(Map<String, String> values) {
        String temperature = values.get("T1H");
        String humidity = values.get("REH");
        String precipitationType = precipitationType(values.get("PTY"));
        String rain = values.get("RN1");
        String wind = values.get("WSD");

        StringBuilder builder = new StringBuilder();
        if (!precipitationType.isBlank()) {
            builder.append(precipitationType);
        } else {
            builder.append("맑거나 강수 없음");
        }
        if (temperature != null && !temperature.isBlank()) {
            builder.append(", 기온 ").append(temperature).append("도");
        }
        if (humidity != null && !humidity.isBlank()) {
            builder.append(", 습도 ").append(humidity).append("%");
        }
        if (rain != null && !rain.isBlank() && !"0".equals(rain)) {
            builder.append(", 1시간 강수량 ").append(rain).append("mm");
        }
        if (wind != null && !wind.isBlank()) {
            builder.append(", 풍속 ").append(wind).append("m/s");
        }
        return builder.toString();
    }

    private String precipitationType(String value) {
        return switch (value == null ? "" : value) {
            case "1" -> "비";
            case "2" -> "비 또는 눈";
            case "3" -> "눈";
            case "5" -> "빗방울";
            case "6" -> "빗방울 또는 눈날림";
            case "7" -> "눈날림";
            default -> "";
        };
    }

    private GridPoint toGrid(double latitude, double longitude) {
        double re = 6371.00877;
        double grid = 5.0;
        double slat1 = Math.toRadians(30.0);
        double slat2 = Math.toRadians(60.0);
        double olon = Math.toRadians(126.0);
        double olat = Math.toRadians(38.0);
        double xo = 43.0;
        double yo = 136.0;

        double sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn);
        double sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sf = Math.pow(sf, sn) * Math.cos(slat1) / sn;
        double ro = Math.tan(Math.PI * 0.25 + olat * 0.5);
        ro = re / grid * sf / Math.pow(ro, sn);

        double ra = Math.tan(Math.PI * 0.25 + Math.toRadians(latitude) * 0.5);
        ra = re / grid * sf / Math.pow(ra, sn);
        double theta = Math.toRadians(longitude) - olon;
        if (theta > Math.PI) theta -= 2.0 * Math.PI;
        if (theta < -Math.PI) theta += 2.0 * Math.PI;
        theta *= sn;

        int x = (int) Math.floor(ra * Math.sin(theta) + xo + 0.5);
        int y = (int) Math.floor(ro - ra * Math.cos(theta) + yo + 0.5);
        return new GridPoint(x, y);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private record GridPoint(int x, int y) {
    }
}
