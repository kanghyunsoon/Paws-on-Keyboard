package com.tour_diary.walk.repository;

import com.tour_diary.walk.domain.WalkRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface WalkRecordRepository extends MongoRepository<WalkRecord, String> {
}
