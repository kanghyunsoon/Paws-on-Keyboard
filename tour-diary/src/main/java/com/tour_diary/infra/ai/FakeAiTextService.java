package com.tour_diary.infra.ai;

import com.tour_diary.ai.text.AiTextService;
import com.tour_diary.ai.text.DiaryTextResult;
import com.tour_diary.diary.domain.DiaryEmotion;
import org.springframework.stereotype.Service;

@Service
public class FakeAiTextService implements AiTextService {

    @Override
    public DiaryTextResult generateDiary(String diaryPrompt) {
        return new DiaryTextResult(
                "보리의 바스락 산책 작전",
                "오늘 나는 바스락바스락 은행잎 길을 만났어.\n"
                        + "처음엔 조금 무서웠지만 냄새를 맡아보니 괜찮은 친구였어.\n"
                        + "벤치 밑에는 아주 비밀스러운 가을 냄새가 숨어 있었어.\n"
                        + "나는 오늘 우리 동네를 멋진 모험길로 정했어.\n"
                        + "집사야, 내일도 노랑 길로 다시 가자!",
                DiaryEmotion.CURIOUS
        );
    }
}
