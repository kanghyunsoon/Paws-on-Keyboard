package com.tour_diary.tourism;

import com.tour_diary.ai.vision.VisionAnalysisResult;
import com.tour_diary.diary.domain.DiaryEmotion;
import com.tour_diary.dog.domain.DogProfile;
import com.tour_diary.walk.domain.WalkRecord;
import java.util.List;

public interface TourismRecommendationService {

    List<RecommendedPlace> recommend(
            DogProfile dog,
            WalkRecord walk,
            VisionAnalysisResult vision,
            DiaryEmotion emotion
    );
}
