package com.tour_diary.diary.repository;

import com.tour_diary.diary.domain.Diary;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DiaryRepository extends MongoRepository<Diary, String> {
    List<Diary> findByDogIdOrderByCreatedAtDesc(String dogId);
    List<Diary> findByUserIdOrderByCreatedAtDesc(String userId);
}
