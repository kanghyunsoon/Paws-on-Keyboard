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
    private final String accessibleTourApiKey;
    private final String accessibleTourBaseUrl;
    private final String greenTourApiKey;
    private final String greenTourBaseUrl;
    private final boolean externalApiEnabled;

    public KtoTourismRecommendationService(
            FakeTourismRecommendationService fallback,
            @Value("${app.kto.tour-api-key:}") String tourApiKey,
            @Value("${app.kto.base-url:https://apis.data.go.kr/B551011/KorService2}") String tourBaseUrl,
            @Value("${app.kto.pet-tour-api-key:}") String petTourApiKey,
            @Value("${app.kto.pet-tour-base-url:https://apis.data.go.kr/B551011/KorPetTourService2}") String petTourBaseUrl,
            @Value("${app.kto.accessible-tour-api-key:}") String accessibleTourApiKey,
            @Value("${app.kto.accessible-tour-base-url:https://apis.data.go.kr/B551011/KorWithService2}") String accessibleTourBaseUrl,
            @Value("${app.kto.green-tour-api-key:}") String greenTourApiKey,
            @Value("${app.kto.green-tour-base-url:https://apis.data.go.kr/B551011/GreenTourService1}") String greenTourBaseUrl,
            @Value("${app.external-api-enabled:true}") boolean externalApiEnabled
    ) {
        this.restClient = RestClient.create();
        this.objectMapper = new ObjectMapper();
        this.fallback = fallback;
        this.tourApiKey = tourApiKey;
        this.tourBaseUrl = tourBaseUrl;
        this.petTourApiKey = petTourApiKey;
        this.petTourBaseUrl = petTourBaseUrl;
        this.accessibleTourApiKey = accessibleTourApiKey;
        this.accessibleTourBaseUrl = accessibleTourBaseUrl;
        this.greenTourApiKey = greenTourApiKey;
        this.greenTourBaseUrl = greenTourBaseUrl;
        this.externalApiEnabled = externalApiEnabled;
    }

    @Override
    public List<RecommendedPlace> recommend(
            DogProfile dog,
            WalkRecord walk,
            VisionAnalysisResult vision,
            DiaryEmotion emotion
    ) {
        if (!externalApiEnabled || walk.latitude() == null || walk.longitude() == null) {
            return fallback.recommend(dog, walk, vision, emotion);
        }

        try {
            List<RecommendedPlace> places = new ArrayList<>();
            places.addAll(fetchPetTourPlaces(dog, walk, vision));
            places.addAll(fetchAccessibleTourPlaces(dog, walk, vision, MAX_RECOMMENDATIONS - places.size()));
            places.addAll(fetchGreenTourPlaces(dog, walk, vision, MAX_RECOMMENDATIONS - places.size()));

            if (places.size() < MAX_RECOMMENDATIONS) {
                places.addAll(fetchGeneralTourPlaces(dog, walk, vision, MAX_RECOMMENDATIONS - places.size()));
            }

            if (places.isEmpty()) {
                return fallback.recommend(dog, walk, vision, emotion);
            }
            return deduplicate(places).stream().limit(MAX_RECOMMENDATIONS).toList();
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
        return parsePlaces(response, dog, vision, SourceKind.PET, MAX_RECOMMENDATIONS);
    }

    private List<RecommendedPlace> fetchAccessibleTourPlaces(
            DogProfile dog,
            WalkRecord walk,
            VisionAnalysisResult vision,
            int limit
    ) {
        if (accessibleTourApiKey == null || accessibleTourApiKey.isBlank() || limit <= 0) {
            return List.of();
        }

        URI uri = locationBasedUri(accessibleTourBaseUrl, "/locationBasedList2", accessibleTourApiKey, walk, Math.max(limit, 5));
        String response = request(uri);
        return parsePlaces(response, dog, vision, SourceKind.ACCESSIBLE, limit);
    }

    private List<RecommendedPlace> fetchGreenTourPlaces(
            DogProfile dog,
            WalkRecord walk,
            VisionAnalysisResult vision,
            int limit
    ) {
        if (greenTourApiKey == null || greenTourApiKey.isBlank() || limit <= 0) {
            return List.of();
        }

        URI uri = basicListUri(greenTourBaseUrl, "/areaBasedList1", greenTourApiKey, Math.max(limit, 5));
        String response = request(uri);
        return parsePlaces(response, dog, vision, SourceKind.GREEN, limit);
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
        return parsePlaces(response, dog, vision, SourceKind.GENERAL, limit);
    }

    private URI basicListUri(String baseUrl, String path, String apiKey, int rows) {
        return URI.create(baseUrl + path
                + "?serviceKey=" + encode(apiKey)
                + "&MobileOS=ETC"
                + "&MobileApp=PawsOnKeyboard"
                + "&_type=json"
                + "&numOfRows=" + rows
                + "&pageNo=1");
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
            SourceKind sourceKind,
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
            places.add(toPlace(items, dog, vision, sourceKind));
            return places;
        }

        if (items.isArray()) {
            for (JsonNode item : items) {
                if (places.size() >= limit) {
                    break;
                }
                places.add(toPlace(item, dog, vision, sourceKind));
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
            SourceKind sourceKind
    ) {
        String title = text(item, "title", "이름 없는 관광지");
        String category = firstText(item, List.of("cat3", "category", "contenttypeid"), "한국관광공사 추천 관광지");
        String address = firstText(item, List.of("addr1", "address"), "");
        String contentId = firstText(item, List.of("contentid", "contentId"), "");
        Integer distance = integer(item, "dist");
        Double latitude = decimal(item, "mapy");
        Double longitude = decimal(item, "mapx");
        String sourceApi = sourceKind.sourceApi;
        String petInfo = switch (sourceKind) {
            case PET -> firstText(item, List.of("petTursmInfo", "petTourInfo", "relaAcdntRiskMtr", "acmpyTypeCd"),
                    "반려동물 동반여행 서비스 기반 후보지입니다. 방문 전 운영 시간과 동반 가능 구역을 확인하세요.");
            case ACCESSIBLE -> "무장애 여행 정보 기반 후보지입니다. 노견, 유모차, 보호자 이동 편의성을 함께 고려할 수 있습니다.";
            case GREEN -> "생태 관광 정보 기반 후보지입니다. 자연, 숲, 생태 체험과 이어지는 산책 후보입니다.";
            case GENERAL -> "국문 관광정보 기반 후보지입니다. 반려동물 동반 규정은 반려동물 동반여행 서비스 또는 현장 안내로 추가 확인이 필요합니다.";
        };

        return new RecommendedPlace(
                title,
                "%s의 %s 분위기와 잘 맞는 한국관광공사 데이터 기반 추천 장소입니다. %s"
                        .formatted(dog.name(), vision.mood(),
                                sourceKind.reasonSuffix),
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

    private List<RecommendedPlace> deduplicate(List<RecommendedPlace> places) {
        List<RecommendedPlace> deduplicated = new ArrayList<>();
        for (RecommendedPlace place : places) {
            boolean exists = deduplicated.stream()
                    .anyMatch(current -> current.name().equals(place.name())
                            || (current.sourceContentId() != null
                            && !current.sourceContentId().isBlank()
                            && current.sourceContentId().equals(place.sourceContentId())));
            if (!exists) {
                deduplicated.add(place);
            }
        }
        return deduplicated;
    }

    private enum SourceKind {
        PET("KorPetTourService2.locationBasedList2", "반려동물 동반 정보가 있는 후보를 우선 반영했습니다."),
        ACCESSIBLE("KorWithService2.locationBasedList2", "이동 편의성과 접근성까지 고려할 수 있는 후보를 반영했습니다."),
        GREEN("GreenTourService1.areaBasedList1", "자연과 생태 경험으로 확장할 수 있는 후보를 반영했습니다."),
        GENERAL("KorService2.locationBasedList2", "주변 관광지 중 산책 동선과 이어질 수 있는 곳을 골랐습니다.");

        private final String sourceApi;
        private final String reasonSuffix;

        SourceKind(String sourceApi, String reasonSuffix) {
            this.sourceApi = sourceApi;
            this.reasonSuffix = reasonSuffix;
        }
    }
}
