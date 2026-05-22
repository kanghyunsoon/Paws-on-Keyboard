export type RecommendedPlace = {
  name: string;
  reason: string;
  category?: string;
  address?: string;
  latitude?: number;
  longitude?: number;
  sourceProvider?: string;
  sourceApi?: string;
  sourceContentId?: string;
  petInfo?: string;
  distanceMeters?: number;
};

export type GenerateDiaryResponse = {
  diaryId: string;
  originalImageUrl: string | null;
  generatedImageUrl: string | null;
  title: string;
  content: string;
  detectedObjects: string[];
  recommendedPlaces: RecommendedPlace[];
};

export type GenerateDiaryRequest = {
  dogId: string;
  walkRecordId: string;
};

export type DiaryDebugResponse = {
  visionResult: string | null;
  diaryPrompt: string | null;
  imagePrompt: string | null;
  tourismPrompt: string | null;
  rawTourismResponse: string | null;
};

export async function generateDiary(request: GenerateDiaryRequest): Promise<GenerateDiaryResponse> {
  const response = await fetch('/api/diaries/generate', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || `일기 생성 요청 실패: ${response.status}`);
  }

  return response.json();
}

export async function getDiaryDebug(diaryId: string): Promise<DiaryDebugResponse> {
  const response = await fetch(`/api/diaries/${diaryId}/debug`);

  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || `디버그 정보 요청 실패: ${response.status}`);
  }

  return response.json();
}
