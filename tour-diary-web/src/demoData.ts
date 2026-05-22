import type { DiaryDebugResponse, GenerateDiaryResponse } from './api';

export const demoDiary: GenerateDiaryResponse = {
  diaryId: 'demo-10',
  originalImageUrl: null,
  generatedImageUrl: null,
  title: '보리의 바스락 산책 작전',
  content:
    '오늘 나는 바스락바스락 은행잎 길을 만났어.\n처음엔 조금 무서웠지만 냄새를 맡아보니 괜찮은 친구였어.\n벤치 밑에는 아주 비밀스러운 가을 냄새가 숨어 있었어.\n나는 오늘 우리 동네를 멋진 모험길로 정했어.\n집사야, 내일도 노랑 길로 다시 가자!',
  detectedObjects: ['강아지', '은행잎', '벤치', '산책로'],
  recommendedPlaces: [
    {
      name: '서울숲',
      reason: '오늘 산책처럼 조용한 공원 분위기를 좋아하는 보리에게 넓은 산책로와 자연 냄새가 잘 맞는 다음 산책지예요.',
      category: '반려견 동반 산책지',
      address: '서울특별시 성동구 뚝섬로 273',
      latitude: 37.5444,
      longitude: 127.0374,
      sourceProvider: '한국관광공사',
      sourceApi: 'locationBasedList2 + detailPetTour2',
      sourceContentId: '126508',
      petInfo: '야외 산책 중심으로 이용하기 좋고, 방문 전 현장별 반려동물 동반 가능 구역 확인이 필요합니다.',
      distanceMeters: 850,
    },
    {
      name: '응봉산 팔각정',
      reason: '사진에서 보인 가을 분위기와 잘 어울리고, 짧은 산책 후 전망을 볼 수 있어 호기심 많은 강아지에게 좋은 후보예요.',
      category: '근교 산책 관광지',
      address: '서울특별시 성동구 금호동4가',
      sourceProvider: '한국관광공사',
      sourceApi: 'locationBasedList2',
      sourceContentId: '127480',
      petInfo: '계단과 경사가 있어 노령견은 이동 부담을 확인해야 합니다.',
      distanceMeters: 2100,
    },
  ],
};

export const demoDebug: DiaryDebugResponse = {
  visionResult: `{
  "dogExists": true,
  "objects": ["강아지", "은행잎", "벤치", "산책로"],
  "colors": ["노랑", "갈색", "하늘색"],
  "mood": "조용하고 평화로운 가을 산책",
  "placeType": "공원 산책로",
  "stickerCandidates": ["은행잎", "벤치", "강아지 발자국"],
  "diaryHints": ["은행잎을 처음 발견한 장면", "조용한 산책 분위기"]
}`,
  diaryPrompt: `너는 반려견의 시점으로 그림일기를 써주는 AI다.

[강아지 정보]
이름: 보리
성격: 호기심이 많고 조심스럽지만 냄새 맡기를 좋아함

[작성 규칙]
- 강아지 1인칭 시점
- 5문장 이내
- 귀엽고 따뜻하게 작성`,
  imagePrompt: `Create a cute hand-drawn crayon-style diary illustration from a dog's point of view.

Scene:
The dog is walking on a quiet autumn park path with yellow leaves.

Style:
- childlike crayon drawing
- pastel colors
- cute and imperfect
- no photorealism`,
  tourismPrompt: `너는 반려견 동반 여행 큐레이터다.

[목표]
한국관광공사 OpenAPI 후보지 중에서 오늘 산책 사진, 강아지 성향, 감정에 맞는 다음 산책/여행지를 추천한다.

[KTO 후보 데이터]
- 서울숲 / locationBasedList2 + detailPetTour2 / 반려견 동반 산책지
- 응봉산 팔각정 / locationBasedList2 / 근교 산책 관광지`,
  rawTourismResponse: `[
  {
    "sourceProvider": "한국관광공사",
    "sourceApi": "locationBasedList2 + detailPetTour2",
    "contentId": "126508",
    "title": "서울숲"
  }
]`,
};
