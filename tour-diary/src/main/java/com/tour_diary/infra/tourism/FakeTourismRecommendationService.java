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
        String location = walk.address() == null || walk.address().isBlank() ? "입력한 장소" : walk.address();
        return List.of(
                new RecommendedPlace(
                        location + " 근처 산책 코스",
                        dog.name() + "가 오늘처럼 보호자와 천천히 걷고 냄새 맡기 좋은 다음 산책 후보입니다.",
                        "반려견 동반 산책지",
                        location,
                        walk.latitude(),
                        walk.longitude(),
                        "사용자 입력 기반 추천",
                        "fallback",
                        null,
                        "실제 추천은 위치 기반 관광 API 연결 후 반려동물 동반 가능 여부를 함께 확인합니다.",
                        null
                ),
                new RecommendedPlace(
                        location + " 주변 쉬어가기 좋은 곳",
                        "오늘 산책 분위기와 이어서 보호자와 잠깐 쉬기 좋은 후보입니다.",
                        "근처 휴식 장소",
                        location,
                        walk.latitude(),
                        walk.longitude(),
                        "사용자 입력 기반 추천",
                        "fallback",
                        null,
                        "방문 전 반려견 출입 가능 여부와 리드줄 규정을 확인해야 합니다.",
                        null
                )
        );
    }
}
