package com.tour_diary.infra.tourism;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tour_diary.ai.vision.VisionAnalysisResult;
import com.tour_diary.diary.domain.DiaryEmotion;
import com.tour_diary.dog.domain.DogProfile;
import com.tour_diary.tourism.RecommendedPlace;
import com.tour_diary.tourism.TourismRecommendationService;
import com.tour_diary.walk.domain.WalkRecord;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Primary
public class KtoTourismRecommendationService implements TourismRecommendationService {

    private static final int SEARCH_RADIUS_METERS = 5000;
    private static final int MAX_RECOMMENDATIONS = 3;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final FakeTourismRecommendationService fallback;
    private final String tourApiKey;
    private final String tourBaseUrl;
    private final String petTourApiKey;
    private final String petTourBaseUrl;

    public KtoTourismRecommendationService(
            FakeTourismRecommendationService fallback,
            @Value("${app.kto.tour-api-key:}") String tourApiKey,
            @Value("${app.kto.base-url:https://apis.data.go.kr/B551011/KorService2}") String tourBaseUrl,
            @Value("${app.kto.pet-tour-api-key:}") String petTourApiKey,
            @Value("${app.kto.pet-tour-base-url:https://apis.data.go.kr/B551011/KorPetTourService2}") String petTourBaseUrl
    ) {
        this.restClient = RestClient.create();
        this.objectMapper = new ObjectMapper();
        this.fallback = fallback;
        this.tourApiKey = tourApiKey;
        this.tourBaseUrl = tourBaseUrl;
        this.petTourApiKey = petTourApiKey;
        this.petTourBaseUrl = petTourBaseUrl;
    }

    @Override
    public List<RecommendedPlace> recommend(
            DogProfile dog,
            WalkRecord walk,
            VisionAnalysisResult vision,
            DiaryEmotion emotion
    ) {
        if (walk.latitude() == null || walk.longitude() == null) {
            return fallback.recommend(dog, walk, vision, emotion);
        }

        try {
            List<RecommendedPlace> places = new ArrayList<>();
            places.addAll(fetchPetTourPlaces(dog, walk, vision));

            if (places.size() < MAX_RECOMMENDATIONS) {
                places.addAll(fetchGeneralTourPlaces(dog, walk, vision, MAX_RECOMMENDATIONS - places.size()));
            }

            if (places.isEmpty()) {
                return fallback.recommend(dog, walk, vision, emotion);
            }
            return places.stream().limit(MAX_RECOMMENDATIONS).toList();
        } catch (RuntimeException ex) {
            return fallback.recommend(dog, walk, vision, emotion);
        }
    }

    private List<RecommendedPlace> fetchPetTourPlaces(
            DogProfile dog,
            WalkRecord walk,
            VisionAnalysisResult vision
    ) {
        if (petTourApiKey == null || petTourApiKey.isBlank()) {
            return List.of();
        }

        URI uri = locationBasedUri(petTourBaseUrl, "/locationBasedList2", petTourApiKey, walk, 5);
        String response = request(uri);
        return parsePlaces(response, dog, vision, true, MAX_RECOMMENDATIONS);
    }

    private List<RecommendedPlace> fetchGeneralTourPlaces(
            DogProfile dog,
            WalkRecord walk,
            VisionAnalysisResult vision,
            int limit
    ) {
        if (tourApiKey == null || tourApiKey.isBlank() || limit <= 0) {
            return List.of();
        }

        URI uri = locationBasedUri(tourBaseUrl, "/locationBasedList2", tourApiKey, walk, Math.max(limit, 5));
        String response = request(uri);
        return parsePlaces(response, dog, vision, false, limit);
    }

    private URI locationBasedUri(String baseUrl, String path, String apiKey, WalkRecord walk, int rows) {
        return URI.create(baseUrl + path
                + "?serviceKey=" + encode(apiKey)
                + "&MobileOS=ETC"
                + "&MobileApp=PawsOnKeyboard"
                + "&_type=json"
                + "&arrange=E"
                + "&numOfRows=" + rows
                + "&pageNo=1"
                + "&mapX=" + walk.longitude()
                + "&mapY=" + walk.latitude()
                + "&radius=" + SEARCH_RADIUS_METERS);
    }

    private String request(URI uri) {
        return restClient.get()
                .uri(uri)
                .retrieve()
                .body(String.class);
    }

    private List<RecommendedPlace> parsePlaces(
            String response,
            DogProfile dog,
            VisionAnalysisResult vision,
            boolean petTour,
            int limit
    ) {
        List<RecommendedPlace> places = new ArrayList<>();
        JsonNode items = readItems(response);

        if (items == null || items.isMissingNode()) {
            return places;
        }

        if (items.isObject()) {
            items = items.path("item");
        }

        if (items.isObject()) {
            places.add(toPlace(items, dog, vision, petTour));
            return places;
        }

        if (items.isArray()) {
            for (JsonNode item : items) {
                if (places.size() >= limit) {
                    break;
                }
                places.add(toPlace(item, dog, vision, petTour));
            }
        }

        return places;
    }

    private JsonNode readItems(String response) {
        try {
            return objectMapper.readTree(response)
                    .path("response")
                    .path("body")
                    .path("items")
                    .path("item");
        } catch (Exception ex) {
            return null;
        }
    }

    private RecommendedPlace toPlace(
            JsonNode item,
            DogProfile dog,
            VisionAnalysisResult vision,
            boolean petTour
    ) {
        String title = text(item, "title", "이름 없는 관광지");
        String category = firstText(item, List.of("cat3", "category", "contenttypeid"), "한국관광공사 추천 관광지");
        String address = firstText(item, List.of("addr1", "address"), "");
        String contentId = firstText(item, List.of("contentid", "contentId"), "");
        Integer distance = integer(item, "dist");
        Double latitude = decimal(item, "mapy");
        Double longitude = decimal(item, "mapx");
        String sourceApi = petTour ? "KorPetTourService2.locationBasedList2" : "KorService2.locationBasedList2";
        String petInfo = petTour
                ? firstText(item, List.of("petTursmInfo", "petTourInfo", "relaAcdntRiskMtr", "acmpyTypeCd"), "반려동물 동반여행 서비스 기반 후보지입니다. 방문 전 운영 시간과 동반 가능 구역을 확인하세요.")
                : "국문 관광정보 기반 후보지입니다. 반려동물 세부 규정은 반려동물 동반여행 서비스 또는 현장 안내로 추가 확인이 필요합니다.";

        return new RecommendedPlace(
                title,
                "%s의 %s 분위기와 잘 맞는 한국관광공사 데이터 기반 추천 장소예요. %s"
                        .formatted(dog.name(), vision.mood(), petTour ? "반려동물 동반 정보가 있는 후보를 우선 반영했어요." : "주변 관광지 중 산책 동선과 이어질 수 있는 곳을 골랐어요."),
                category,
                address,
                latitude,
                longitude,
                "한국관광공사",
                sourceApi,
                contentId,
                petInfo,
                distance
        );
    }

    private String firstText(JsonNode node, List<String> fields, String fallbackValue) {
        for (String field : fields) {
            String value = text(node, field, "");
            if (!value.isBlank()) {
                return value;
            }
        }
        return fallbackValue;
    }

    private String text(JsonNode node, String field, String fallbackValue) {
        String value = node.path(field).asText();
        return value == null || value.isBlank() ? fallbackValue : value;
    }

    private Integer integer(JsonNode node, String field) {
        if (node.path(field).isMissingNode() || node.path(field).asText().isBlank()) {
            return null;
        }
        return node.path(field).asInt();
    }

    private Double decimal(JsonNode node, String field) {
        if (node.path(field).isMissingNode() || node.path(field).asText().isBlank()) {
            return null;
        }
        return node.path(field).asDouble();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
