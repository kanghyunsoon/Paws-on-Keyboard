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
                "보리의 바삭바삭 산책 작전",
                "오늘 나는 바삭바삭한 낙엽 괴물을 만났다.\n"
                        + "처음엔 조금 무서웠지만 냄새를 맡아보니 괜찮은 친구였다.\n"
                        + "벤치 밑에는 엄청난 비밀 냄새도 숨어 있었다.\n"
                        + "나는 오늘도 우리 동네를 멋지게 순찰했다.\n"
                        + "집사야, 내일도 나랑 여기 또 오자멍!",
                DiaryEmotion.CURIOUS
        );
    }
}
