# Paws-on-Keyboard Project Blueprint

## 결론

현재 구조로 끝까지 진행한다. 단, 이 프로젝트의 중심은 단순 AI 일기장이 아니라 `관광데이터와 연결되는 반려견 시점 AI 그림일기`다.

따라서 모든 구현 우선순위는 아래 핵심 플로우를 기준으로 판단한다.

```text
산책 사진
-> Vision 분석
-> 강아지 프로필 + 산책 정보 + 사진 분석 조합
-> 강아지 시점 일기 생성
-> 강아지가 그린 듯한 크레파스 그림일기 생성
-> 한국관광공사 관광데이터 추천
-> 일기장 저장/조회
```

## Repository Structure

```text
Paws-on-Keyboard/
├─ tour-diary/        # Spring Boot backend
├─ tour-diary-web/    # React frontend
└─ PROJECT_BLUEPRINT.md
```

프론트와 백엔드는 분리한다. 프롬프톤 MVP에서는 개발 속도, API 계약, 발표 화면 구성을 위해 이 방식이 가장 깔끔하다.

## Backend Architecture

백엔드는 Spring Boot + MongoDB 기준으로 간다.

```text
com.tour_diary
├─ dog                # 강아지 프로필
├─ walk               # 산책 기록, 원본 사진, 위치, 날씨
├─ diary              # 그림일기 생성/저장/조회
├─ ai                 # AI 포트
│  ├─ vision
│  ├─ text
│  └─ image
├─ prompt             # 프롬프트 조립
├─ tourism            # 관광 추천 포트
└─ infra              # 외부 API 구현체
   ├─ ai
   └─ tourism
```

### 중요한 설계 원칙

1. `DiaryGenerationService`가 MVP 생성 플로우의 오케스트레이터다.
2. Controller는 요청/응답만 담당한다.
3. AI/Vision/KTO 연동은 인터페이스를 먼저 두고 `infra` 구현체로 교체한다.
4. 프롬프트는 `DiaryPromptBuilder`, `ImagePromptBuilder`에 모은다.
5. `diaryPrompt`, `imagePrompt`는 반드시 저장한다.
6. 이미지 생성은 부가 기능이 아니라 핵심 기능이다.

## Backend Generation Flow

```text
DiaryController
-> DiaryGenerationService
   -> DogProfileRepository
   -> WalkRecordRepository
   -> VisionService
   -> DiaryPromptBuilder
   -> AiTextService
   -> ImagePromptBuilder
   -> AiImageService
   -> TourismRecommendationService
   -> DiaryRepository
```

## Core API Contract

### Generate Diary

```http
POST /api/diaries/generate
Content-Type: application/json
```

```json
{
  "dogId": "1",
  "walkRecordId": "3"
}
```

```json
{
  "diaryId": "10",
  "originalImageUrl": "/uploads/walk-3.jpg",
  "generatedImageUrl": "/uploads/generated/diary-10.png",
  "title": "보리의 낙엽 괴물 탐험",
  "content": "오늘 나는 바삭바삭한 낙엽 괴물을 만났다...",
  "detectedObjects": ["강아지", "낙엽", "벤치", "산책로"],
  "recommendedPlaces": [
    {
      "name": "서울숲",
      "reason": "산책로가 넓고 반려견과 걷기 좋아요.",
      "category": "반려견 동반 산책지",
      "address": "서울특별시 성동구",
      "latitude": 37.5444,
      "longitude": 127.0374
    }
  ]
}
```

### Diary Debug

발표와 디버깅을 위해 추가해야 하는 API다.

```http
GET /api/diaries/{diaryId}/debug
```

응답에는 아래 항목을 포함한다.

```text
visionResult
diaryPrompt
imagePrompt
tourismPrompt
rawTourismResponse
```

## MongoDB Collections

### dogs

```text
id
name
personality
favoriteThings
speakingStyle
age
```

### walk_records

```text
id
dogId
originalImageUrl
latitude
longitude
address
weather
temperature
walkedAt
```

### diaries

```text
id
walkRecordId
dogId
title
content
emotion
detectedObjects
diaryPrompt
imagePrompt
generatedImageUrl
recommendedPlaces
createdAt
```

나중에 Vision 분석 원문을 저장하려면 아래 필드를 추가한다.

```text
visionAnalysis
tourismPrompt
rawTourismResponse
```

## Frontend Architecture

프론트는 `tour-diary-web`에서 React + Vite + TypeScript로 간다.

```text
tour-diary-web/src
├─ api.ts             # 백엔드 API 클라이언트
├─ App.tsx            # MVP 화면
├─ demoData.ts        # 백엔드 미완성 구간용 데모 데이터
├─ main.tsx
└─ styles.css
```

초기 MVP 화면은 아래 흐름을 우선한다.

```text
강아지/산책 기록 선택
-> 그림일기 생성
-> 원본 사진과 AI 그림 나란히 표시
-> 오늘의 일기 표시
-> 추천 관광지 표시
-> 프롬프트 디버그 탭 표시
```

백엔드 CRUD와 이미지 업로드가 붙으면 프론트는 아래 구조로 확장한다.

```text
src
├─ api
├─ components
├─ pages
│  ├─ DogProfilePage
│  ├─ WalkUploadPage
│  ├─ DiaryGeneratePage
│  ├─ DiaryDetailPage
│  └─ DiaryListPage
└─ styles
```

## Implementation Order

### Phase 1: Local MVP Skeleton

1. Dog CRUD
2. WalkRecord CRUD
3. 이미지 업로드
4. `/api/diaries/generate`
5. 프론트 데모 화면

### Phase 2: AI Pipeline

1. Vision API 실제 연동
2. 일기 생성 API 실제 연동
3. 이미지 생성 API 또는 로컬 이미지 서버 연동
4. 생성 이미지 파일 저장
5. 프롬프트 디버그 API

### Phase 3: Tourism Data

1. Kakao 위치/주소 처리
2. KTO TourAPI 연동
3. 두루누비 API 연동
4. 무장애 관광정보 API 연동
5. AI 추천 이유 생성

### Phase 4: Product Finish

1. 일기장 목록/상세
2. 모바일 화면 정리
3. 발표용 프롬프트 설계 화면
4. 예외 처리와 로딩 상태
5. 시연용 샘플 데이터

## Do Not Change Without Reason

아래 결정은 프로젝트 끝까지 유지한다.

1. 프론트/백엔드는 분리한다.
2. AI 그림 생성은 핵심 기능이다.
3. 생성 API는 `POST /api/diaries/generate` 하나로 묶는다.
4. 외부 API는 `infra` 구현체로 격리한다.
5. 프롬프트는 저장하고 화면에서 보여줄 수 있게 한다.
6. MongoDB 문서 모델로 빠르게 MVP를 완성한다.
