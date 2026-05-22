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
                상황 힌트: %s

                [작성 규칙]
                - 강아지 1인칭 시점으로 쓴다.
                - 5문장 이내로 작성한다.
                - 너무 사람처럼 설명하지 않는다.
                - 귀엽고 따뜻하게 쓴다.
                - 마지막 문장은 집사에게 말을 거는 느낌으로 끝낸다.
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
