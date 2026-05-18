package com.tour_diary.dog.domain;

import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "dogs")
public record DogProfile(
        @Id String id,
        String name,
        String personality,
        List<String> favoriteThings,
        String speakingStyle,
        Integer age
) {
}
