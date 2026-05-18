package com.tour_diary.dog.repository;

import com.tour_diary.dog.domain.DogProfile;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DogProfileRepository extends MongoRepository<DogProfile, String> {
}
