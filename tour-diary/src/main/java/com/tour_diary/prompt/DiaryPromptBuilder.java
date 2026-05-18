package com.tour_diary.prompt;

import com.tour_diary.ai.vision.VisionAnalysisResult;
import com.tour_diary.dog.domain.DogProfile;
import com.tour_diary.walk.domain.WalkRecord;
import org.springframework.stereotype.Component;

@Component
public class DiaryPromptBuilder {

    public String build(DogProfile dog, WalkRecord walk, VisionAnalysisResult vision) {
        return """
                너는 반려견의 시점으로 그림일기를 써주는 AI다.

                [강아지 정보]
                이름: %s
                성격: %s
                좋아하는 것: %s
                말투: %s

                [오늘 산책 정보]
                날씨: %s
                기온: %s도
                장소: %s

                [사진 분석 결과]
                객체: %s
                분위기: %s
                장소 유형: %s
                상황 단서: %s

                [작성 규칙]
                - 강아지 1인칭 시점
                - 5문장 이내
                - 너무 사람처럼 똑똑하게 쓰지 않기
                - 귀엽고 엉뚱하게 쓰기
                - 마지막 문장은 집사에게 말 거는 느낌
                """.formatted(
                dog.name(),
                dog.personality(),
                dog.favoriteThings(),
                dog.speakingStyle(),
                walk.weather(),
                walk.temperature(),
                walk.address(),
                vision.objects(),
                vision.mood(),
                vision.placeType(),
                vision.diaryHints()
        );
    }
}
