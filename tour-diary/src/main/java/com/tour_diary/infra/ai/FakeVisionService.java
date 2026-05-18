package com.tour_diary.infra.ai;

import com.tour_diary.ai.vision.VisionAnalysisResult;
import com.tour_diary.ai.vision.VisionService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class FakeVisionService implements VisionService {

    @Override
    public VisionAnalysisResult analyze(String imageUrl) {
        return new VisionAnalysisResult(
                true,
                List.of("강아지", "낙엽", "벤치", "산책로"),
                List.of("노란색", "갈색", "하늘색"),
                "따뜻하고 평화로운 가을 산책",
                "공원 산책로",
                List.of("낙엽", "벤치", "강아지 발자국"),
                List.of("낙엽을 처음 발견한 듯한 장면", "조용한 산책 분위기")
        );
    }
}
