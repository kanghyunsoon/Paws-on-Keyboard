package com.tour_diary.infra.map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tour_diary.map.PlaceResolution;
import com.tour_diary.map.PlaceResolver;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Primary
public class KakaoPlaceResolver implements PlaceResolver {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String restApiKey;
    private final boolean externalApiEnabled;

    public KakaoPlaceResolver(
            @Value("${app.kakao.rest-api-key:}") String restApiKey,
            @Value("${app.external-api-enabled:true}") boolean externalApiEnabled
    ) {
        this.restClient = RestClient.create();
        this.objectMapper = new ObjectMapper();
        this.restApiKey = restApiKey;
        this.externalApiEnabled = externalApiEnabled;
    }

    @Override
    public Optional<PlaceResolution> resolve(String query) {
        if (!externalApiEnabled || restApiKey == null || restApiKey.isBlank() || query == null || query.isBlank()) {
            return Optional.empty();
        }

        return request("/v2/local/search/keyword.json", query)
                .or(() -> request("/v2/local/search/address.json", query));
    }

    private Optional<PlaceResolution> request(String path, String query) {
        try {
            String response = restClient.get()
                    .uri(URI.create("https://dapi.kakao.com" + path + "?query=" + encode(query) + "&size=1"))
                    .header("Authorization", "KakaoAK " + restApiKey)
                    .retrieve()
                    .body(String.class);

            JsonNode first = objectMapper.readTree(response).path("documents").path(0);
            if (first.isMissingNode()) {
                return Optional.empty();
            }

            String name = firstText(first, "place_name", "address_name", query);
            String address = firstText(first, "road_address_name", "address_name", name);
            Double longitude = decimal(first, "x");
            Double latitude = decimal(first, "y");
            if (latitude == null || longitude == null) {
                return Optional.empty();
            }
            return Optional.of(new PlaceResolution(name, address, latitude, longitude, "Kakao Local"));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private String firstText(JsonNode node, String firstField, String secondField, String fallback) {
        String first = node.path(firstField).asText();
        if (first != null && !first.isBlank()) {
            return first;
        }
        String second = node.path(secondField).asText();
        return second == null || second.isBlank() ? fallback : second;
    }

    private Double decimal(JsonNode node, String field) {
        String value = node.path(field).asText();
        if (value == null || value.isBlank()) {
            return null;
        }
        return Double.parseDouble(value);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
