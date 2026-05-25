# Tour Diary Backend Architecture

## Purpose

`tour-diary` is the Spring Boot backend for Paws-on-Keyboard. It generates a pet diary from a walk record, creates or references a diary-style image, recommends nearby pet-friendly places, and stores the result for retrieval and debugging.

## Generation Flow

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

## Package Roles

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
   ├─ config
   └─ tourism
```

The `ai` and `tourism` packages define ports. Implementations that call external providers live under `infra`.

## External Provider Strategy

Primary implementations call external services when configured:

- `GeminiVisionService`
- `GroqAiTextService`
- `GeminiAiTextService`
- `CloudflareAiImageService`
- `KtoTourismRecommendationService`

Fallback implementations keep the MVP usable without API keys:

- `FakeVisionService`
- `FakeAiTextService`
- `FakeAiImageService`
- `FakeTourismRecommendationService`

Set `APP_EXTERNAL_API_ENABLED=false` to force fallback mode even when `.env` contains real API keys.

## Main Endpoints

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

### Get Diary

```http
GET /api/diaries/{diaryId}
```

### Get Debug Data

```http
GET /api/diaries/{diaryId}/debug
```

The debug response contains the vision result, diary prompt, image prompt, tourism prompt, and raw tourism candidate data.

## Data Model

### DogProfile

```text
id
name
personality
favoriteThings
speakingStyle
age
```

### WalkRecord

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

### Diary

```text
id
walkRecordId
dogId
title
content
emotion
detectedObjects
visionAnalysis
diaryPrompt
imagePrompt
tourismPrompt
rawTourismResponse
generatedImageUrl
recommendedPlaces
createdAt
```

## Local Commands

```powershell
.\gradlew.bat test
```

```powershell
$env:APP_EXTERNAL_API_ENABLED='false'
.\gradlew.bat bootRun
```
