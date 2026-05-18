export type RecommendedPlace = {
  name: string;
  reason: string;
  category?: string;
  address?: string;
  latitude?: number;
  longitude?: number;
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
