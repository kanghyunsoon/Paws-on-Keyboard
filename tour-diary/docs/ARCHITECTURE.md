# 댕댕이 투어 일기 MVP 아키텍처

## 핵심 정의

댕댕이 투어 일기는 산책 사진을 기반으로 AI가 강아지 시점의 일기와 크레파스 그림일기를 생성하고, 한국관광공사 공공데이터를 활용해 다음 반려견 동반 산책지와 무장애 관광 코스를 추천하는 AI 관광 기록 서비스다.

## 1차 MVP 흐름

```text
사진 업로드
-> Vision 분석
-> 강아지 프로필 + 날씨 + 위치 + 사진 분석 결과 조합
-> 강아지 시점 일기 생성
-> 일기 내용 기반 AI 그림일기 생성
-> KTO 관광데이터 기반 장소 추천
-> 일기장 저장
```

## Spring 계층

```text
DiaryController
-> DiaryGenerationService
   -> VisionService
   -> DiaryPromptBuilder
   -> AiTextService
   -> ImagePromptBuilder
   -> AiImageService
   -> TourismRecommendationService
   -> DiaryRepository
```

## 패키지 구조

```text
com.tour_diary
├─ dog
│  ├─ domain
│  └─ repository
├─ walk
│  ├─ domain
│  └─ repository
├─ diary
│  ├─ controller
│  ├─ domain
│  ├─ repository
│  └─ service
├─ ai
│  ├─ vision
│  ├─ text
│  └─ image
├─ prompt
├─ tourism
└─ infra
   ├─ ai
   └─ tourism
```

`ai`, `tourism` 패키지는 포트다. 실제 OpenAI, 로컬 이미지 모델, 한국관광공사 API 연동은 `infra` 구현체로 교체한다.

## 대표 API

```http
POST /api/diaries/generate
Content-Type: application/json

{
  "dogId": "1",
  "walkRecordId": "3"
}
```

응답:

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

## MongoDB 컬렉션

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

`diaryPrompt`와 `imagePrompt`는 발표/디버그 화면에서 프롬프트 설계 과정을 보여주기 위해 반드시 저장한다.

## 구현 우선순위

1. Dog CRUD
2. WalkRecord CRUD + 이미지 업로드
3. Vision 분석 결과 저장
4. 일기 생성
5. AI 그림 생성
6. KTO 장소 추천
7. 일기장 저장/조회
8. 프롬프트 디버그 화면
