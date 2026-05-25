package com.tour_diary.community.repository;

import com.tour_diary.community.domain.CommunityPost;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CommunityPostRepository extends MongoRepository<CommunityPost, String> {
    List<CommunityPost> findAllByOrderByCreatedAtDesc();
    List<CommunityPost> findByAuthorIdOrderByCreatedAtDesc(String authorId);
}
