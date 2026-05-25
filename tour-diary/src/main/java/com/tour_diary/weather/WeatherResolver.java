package com.tour_diary.weather;

import java.util.Optional;

public interface WeatherResolver {
    Optional<WeatherSummary> resolve(Double latitude, Double longitude);
}
