import type { GenerateDiaryResponse } from './api';

export const demoDiary: GenerateDiaryResponse = {
  diaryId: 'demo-10',
  originalImageUrl: null,
  generatedImageUrl: null,
  title: '보리의 낙엽 괴물 탐험',
  content:
    '오늘 나는 바삭바삭한 낙엽 괴물을 만났다.\n처음엔 조금 무서웠지만 냄새를 맡아보니 괜찮은 친구였다.\n벤치 밑에는 엄청난 비밀 냄새도 숨어 있었다.\n나는 오늘도 우리 동네를 멋지게 순찰했다.\n집사야, 내일도 나랑 여기 또 오자멍!',
  detectedObjects: ['강아지', '낙엽', '벤치', '산책로'],
  recommendedPlaces: [
    {
      name: '서울숲',
      reason: '오늘 산책처럼 넓은 길과 자연 분위기가 있어 반려견과 다시 걷기 좋아요.',
      category: '반려견 동반 산책지',
      address: '서울특별시 성동구',
      latitude: 37.5444,
      longitude: 127.0374,
    },
    {
      name: '남산 둘레길',
      reason: '호기심 많은 강아지가 냄새를 맡으며 걷기 좋은 완만한 산책 코스예요.',
      category: '두루누비 걷기 코스',
      address: '서울특별시 중구',
    },
  ],
};

export const debugText = {
  vision: `{
  "dogExists": true,
  "objects": ["강아지", "낙엽", "벤치", "산책로"],
  "colors": ["노란색", "갈색", "하늘색"],
  "mood": "따뜻하고 평화로운 가을 산책",
  "placeType": "공원 산책로",
  "stickerCandidates": ["낙엽", "벤치", "강아지 발자국"],
  "diaryHints": ["낙엽을 처음 발견한 듯한 장면", "조용한 산책 분위기"]
}`,
  diaryPrompt: `너는 반려견의 시점으로 그림일기를 써주는 AI다.

[강아지 정보]
이름: 보리
성격: 겁이 많지만 호기심 많음
좋아하는 것: 낙엽, 간식
말투: 애교 많고 약간 허세 있음

[작성 규칙]
- 강아지 1인칭 시점
- 5문장 이내
- 귀엽고 엉뚱하게 쓰기`,
  imagePrompt: `Create a cute hand-drawn crayon-style diary illustration from a dog's point of view.

Diary content:
"오늘 나는 바삭바삭한 낙엽 괴물을 만났다..."

Style:
- childlike crayon drawing
- pastel colors
- slightly crooked lines
- looks like a dog tried to draw its own walk
- no photorealism`,
};
