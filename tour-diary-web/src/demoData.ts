import type { GenerateDiaryResponse } from './api';

export const demoDiary: GenerateDiaryResponse = {
  diaryId: 'demo-10',
  originalImageUrl: null,
  generatedImageUrl: null,
  title: '아빠랑 킁킁 산책!',
  content:
    '오늘은 아빠가 줄을 챙기자마자 내 꼬리가 먼저 붕붕 흔들렸어!\n밖에 나오니까 풀 냄새랑 흙 냄새가 코끝에 와서 나는 계속 킁킁했어.\n아빠가 천천히 걸어줘서 나는 나무 밑이랑 꽃 옆을 마음껏 살펴봤어.\n가끔 아빠를 올려다보면 아빠도 웃고 있어서 내 마음이 폭신폭신했어.\n오늘 산책은 내가 아빠를 데리고 다닌 작은 모험 같았어.\n내일도 아빠 손 잡고 또 킁킁하러 가고 싶어!',
  detectedObjects: ['업로드 사진 속 강아지', '사진 속 배경', '목줄', '산책 장면'],
  recommendedPlaces: [
    {
      name: '입력한 장소 근처 산책 코스',
      reason:
        '강아지가 보호자와 천천히 걷고 냄새 맡기 좋은 다음 산책 후보입니다.',
      category: '반려견 동반 산책지',
      sourceProvider: '사용자 입력 기반 추천',
      sourceApi: 'demo',
      petInfo: '실제 추천은 위치 기반 API 연결 후 반려동물 동반 가능 여부를 함께 확인합니다.',
    },
  ],
};
