package com.tour_diary.walk.controller;

import com.tour_diary.map.PlaceResolution;
import com.tour_diary.map.PlaceResolver;
import com.tour_diary.weather.WeatherResolver;
import com.tour_diary.weather.WeatherSummary;
import com.tour_diary.walk.controller.dto.CreateWalkRecordRequest;
import com.tour_diary.walk.domain.WalkRecord;
import com.tour_diary.walk.repository.WalkRecordRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/walk-records", produces = "application/json; charset=UTF-8")
public class WalkRecordController {

    private final WalkRecordRepository walkRecordRepository;
    private final PlaceResolver placeResolver;
    private final WeatherResolver weatherResolver;

    public WalkRecordController(
            WalkRecordRepository walkRecordRepository,
            PlaceResolver placeResolver,
            WeatherResolver weatherResolver
    ) {
        this.walkRecordRepository = walkRecordRepository;
        this.placeResolver = placeResolver;
        this.weatherResolver = weatherResolver;
    }

    @PostMapping
    public WalkRecord create(@RequestBody CreateWalkRecordRequest request) {
        PlaceResolution resolvedPlace = resolvePlace(request);
        Double latitude = resolvedPlace == null ? request.latitude() : resolvedPlace.latitude();
        Double longitude = resolvedPlace == null ? request.longitude() : resolvedPlace.longitude();
        String weather = resolveWeather(request.weather(), latitude, longitude);
        WalkRecord walk = new WalkRecord(
                null,
                textOrDefault(request.dogId(), "current-dog"),
                text(request.originalImageUrl()),
                latitude,
                longitude,
                resolvedPlace == null ? text(request.address()) : resolvedAddress(request.address(), resolvedPlace),
                weather,
                null,
                Instant.now()
        );

        try {
            return walkRecordRepository.save(walk);
        } catch (DataAccessException ex) {
            return new WalkRecord(
                    "preview-walk-" + Instant.now().toEpochMilli(),
                    walk.dogId(),
                    walk.originalImageUrl(),
                    walk.latitude(),
                    walk.longitude(),
                    walk.address(),
                    walk.weather(),
                    walk.temperature(),
                    walk.walkedAt()
            );
        }
    }

    @GetMapping
    public List<WalkRecord> list(@RequestParam(value = "dogId", defaultValue = "current-dog") String dogId) {
        try {
            return walkRecordRepository.findByDogIdOrderByWalkedAtDesc(dogId);
        } catch (DataAccessException ex) {
            return List.of();
        }
    }

    private String text(String value) {
        return value == null ? "" : value.trim();
    }

    private String textOrDefault(String value, String fallback) {
        String clean = text(value);
        return clean.isBlank() ? fallback : clean;
    }

    private PlaceResolution resolvePlace(CreateWalkRecordRequest request) {
        if (request.latitude() != null && request.longitude() != null) {
            return null;
        }
        String address = text(request.address());
        if (address.isBlank()) {
            return null;
        }
        return placeResolver.resolve(address).orElse(null);
    }

    private String resolvedAddress(String originalAddress, PlaceResolution resolvedPlace) {
        String original = text(originalAddress);
        String resolved = textOrDefault(resolvedPlace.address(), resolvedPlace.name());
        if (original.isBlank() || original.equals(resolved)) {
            return resolved;
        }
        return original + " / 좌표 기준: " + resolved;
    }

    private String resolveWeather(String inputWeather, Double latitude, Double longitude) {
        String cleanWeather = text(inputWeather);
        if (!cleanWeather.isBlank()) {
            return cleanWeather;
        }
        return weatherResolver.resolve(latitude, longitude)
                .map(WeatherSummary::description)
                .orElse("");
    }
}
