package com.tour_diary.infra.ai;

import com.tour_diary.ai.text.AiTextService;
import com.tour_diary.ai.text.DiaryTextResult;
import com.tour_diary.diary.domain.DiaryEmotion;
import org.springframework.stereotype.Service;

@Service
public class FakeAiTextService implements AiTextService {

    @Override
    public DiaryTextResult generateDiary(String diaryPrompt) {
        String dogName = valueAfter(diaryPrompt, "이름:", "나");
        String age = valueAfter(diaryPrompt, "나이:", "");
        String personality = valueAfter(diaryPrompt, "성격:", "호기심 많은 성격");
        String place = valueAfter(diaryPrompt, "장소:", "사진 속 산책길");
        String weather = valueAfter(diaryPrompt, "날씨:", "기분 좋은 날씨");
        String scene = valueAfter(diaryPrompt, "상황 요약:", "보호자와 함께 보낸 산책 순간");
        String objects = valueAfter(diaryPrompt, "핵심 객체:", "강아지, 산책길, 보호자 단서");
        String action = valueAfter(diaryPrompt, "강아지 자세/행동:", "주변을 살피는 모습");
        String viewpoint = valueAfter(diaryPrompt, "강아지 시점 단서:", "낮은 시선에서 기억한 장면");
        String owner = ownerFrom(diaryPrompt);

        String ageTone = age.matches(".*\\b(8|9|10|11|12|13|14|15|16|17|18|19|20)\\b.*")
                ? "천천히 오래 냄새를 맡으니까 마음이 포근했다."
                : "궁금한 냄새가 많아서 발이 먼저 통통 움직였다.";
        String personalityLine = personality.isBlank()
                ? ""
                : "나는 " + personality + "라서 그 순간을 더 크게 기억했다.";

        String content = """
                오늘 나는 %s와 %s에 갔다.
                %s였고, 사진 속에는 %s이 보였다.
                %s
                %s
                %s
                %s, 다음에도 내가 본 이 장면을 또 같이 기억해 줘.
                """.formatted(
                owner,
                place,
                weather,
                objects,
                scene + "이 내 코끝에 남았다.",
                action + "으로 " + viewpoint + "을 떠올렸다.",
                ageTone + " " + personalityLine,
                owner
        ).trim();

        return new DiaryTextResult(
                dogName + "의 " + place + " 그림일기",
                content,
                DiaryEmotion.HAPPY
        );
    }

    private String valueAfter(String text, String label, String fallback) {
        int start = text.indexOf(label);
        if (start < 0) {
            return fallback;
        }
        int valueStart = start + label.length();
        int end = text.indexOf('\n', valueStart);
        String value = (end < 0 ? text.substring(valueStart) : text.substring(valueStart, end)).trim();
        return value.isBlank() ? fallback : value;
    }

    private String ownerFrom(String text) {
        String marker = "말투와 보호자 관계:";
        int start = text.indexOf(marker);
        if (start < 0) {
            return "보호자";
        }
        int valueStart = start + marker.length();
        int end = text.indexOf('\n', valueStart);
        String value = (end < 0 ? text.substring(valueStart) : text.substring(valueStart, end)).trim();
        for (String role : new String[]{"엄마", "아빠", "누나", "언니", "형", "오빠", "친구"}) {
            if (value.contains(role)) {
                return role;
            }
        }
        return "보호자";
    }
}
