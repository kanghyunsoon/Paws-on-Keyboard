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
        return List.of(
                new RecommendedPlace(
                        "서울숲",
                        dog.name() + "가 오늘처럼 조용한 공원 산책을 좋아해서, 넓은 산책로와 자연 분위기가 잘 맞는 다음 산책지예요.",
                        "반려견 동반 산책지",
                        "서울특별시 성동구 뚝섬로 273",
                        37.5444,
                        127.0374,
                        "한국관광공사",
                        "locationBasedList2 + detailPetTour2",
                        "126508",
                        "야외 산책 중심으로 이용하기 좋고, 방문 전 현장별 반려동물 동반 가능 구역 확인이 필요합니다.",
                        850
                ),
                new RecommendedPlace(
                        "응봉산 팔각정",
                        "사진에서 보인 가을 분위기와 잘 어울리고, 짧은 산책 후 전망을 볼 수 있어 호기심 많은 강아지에게 좋은 후보예요.",
                        "근교 산책 관광지",
                        "서울특별시 성동구 금호동4가",
                        37.5481,
                        127.0293,
                        "한국관광공사",
                        "locationBasedList2",
                        "127480",
                        "계단과 경사가 있어 노령견은 이동 부담을 확인해야 합니다.",
                        2100
                )
        );
    }
}
