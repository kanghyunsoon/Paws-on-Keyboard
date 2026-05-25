package com.tour_diary.community.repository;

import com.tour_diary.community.domain.FollowRelation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface FollowRelationRepository extends MongoRepository<FollowRelation, String> {
    Optional<FollowRelation> findByFollowerIdAndFollowingId(String followerId, String followingId);
    List<FollowRelation> findByFollowerId(String followerId);
    List<FollowRelation> findByFollowingId(String followingId);
}
