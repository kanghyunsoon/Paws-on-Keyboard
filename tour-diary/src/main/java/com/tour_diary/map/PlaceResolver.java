package com.tour_diary.map;

import java.util.Optional;

public interface PlaceResolver {
    Optional<PlaceResolution> resolve(String query);
}
