package com.tour_diary.infra.tourism;

import com.tour_diary.ai.vision.VisionAnalysisResult;
import com.tour_diary.diary.domain.DiaryEmotion;
import com.tour_diary.dog.domain.DogProfile;
import com.tour_diary.tourism.RecommendedPlace;
import com.tour_diary.tourism.TourismRecommendationService;
import com.tour_diary.walk.domain.WalkRecord;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class FakeTourismRecommendationService implements TourismRecommendationService {

    @Override
    public List<RecommendedPlace> recommend(
            DogProfile dog,
            WalkRecord walk,
            VisionAnalysisResult vision,
            DiaryEmotion emotion
    ) {
        return List.of(new RecommendedPlace(
                "서울숲",
                dog.name() + "가 좋아한 산책 분위기와 비슷하고, 넓은 산책로가 있어 다음 산책지로 좋아요.",
                "반려견 동반 산책지",
                "서울특별시 성동구",
                37.5444,
                127.0374
        ));
    }
}
