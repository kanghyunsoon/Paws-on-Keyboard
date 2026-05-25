# API Keys

API 키는 프론트엔드에 넣지 않습니다. 서버 전용 키는 `tour-diary/.env` 또는 배포 환경 변수에만 둡니다.

## Current Backend Keys

`tour-diary/.env.example`을 복사해 `tour-diary/.env`를 만들고 필요한 값만 채웁니다.

```properties
APP_EXTERNAL_API_ENABLED=true
APP_GENERATION_DAILY_LIMIT=3

GEMINI_API_KEY=
GEMINI_MODEL=gemini-2.5-flash

GROQ_API_KEY=
GROQ_MODEL=llama-3.1-8b-instant

CLOUDFLARE_ACCOUNT_ID=
CLOUDFLARE_API_TOKEN=
CLOUDFLARE_IMAGE_MODEL=@cf/black-forest-labs/flux-1-schnell

KTO_TOUR_API_KEY=
KTO_TOUR_API_BASE_URL=https://apis.data.go.kr/B551011/KorService2

KTO_PET_TOUR_API_KEY=
KTO_PET_TOUR_API_BASE_URL=https://apis.data.go.kr/B551011/KorPetTourService2

KTO_DURUNUBI_API_KEY=
KTO_ACCESSIBLE_TOUR_API_KEY=
KTO_GREEN_TOUR_API_KEY=

KAKAO_REST_API_KEY=

MONGODB_URI=mongodb://localhost:27017/tour-diary
MONGODB_DATABASE=tour-diary
```

## API Issuance Checklist

공식 발급 경로:

- 공공데이터포털: https://www.data.go.kr
- Kakao Developers: https://developers.kakao.com
- 한국관광공사 TourAPI 학습/소개: https://touredu.visitkorea.or.kr

필요한 신청 항목:

1. 공공데이터포털 회원가입/로그인
2. 한국관광공사 국문 관광정보 서비스 활용신청
3. 한국관광공사 반려동물 동반여행 서비스 활용신청
4. 두루누비 정보 서비스 활용신청
5. 무장애 여행 정보 활용신청
6. 생태 관광 정보 활용신청
7. 기상청 단기예보/현재날씨 API 활용신청
8. Kakao Developers 앱 생성
9. Kakao REST API 키 발급
10. Kakao 허용 도메인/허용 IP 제한

## Missing/Blocked Integrations

현재 코드가 실제로 완성하려면 다음 키가 필요합니다.

- 한국관광공사 TourAPI 키: 공모전 데이터 활용 필수
- 한국관광공사 반려동물 동반여행 서비스 키: 반려견 동반 장소 추천 필수
- 두루누비 키: 산책길/코스 추천 강화
- 무장애 여행 키: 유모차, 노견, 보호자 접근성 추천 강화
- 생태 관광 키: 자연/공원/숲 기반 추천 강화
- Kakao REST API 키: 장소 검색, 좌표 변환, 거리 계산
- 기상청 API 키: 날씨 자동 입력
- image-to-image 지원 이미지 생성 API 키: 실제 사진과 같은 강아지/주인을 유지한 그림 생성

## Fallback Behavior

API 키가 없어도 MVP 화면과 데모는 동작합니다.

- Vision: `FakeVisionService`
- Text: `FakeAiTextService`
- Image: `FakeAiImageService`
- Tourism: `FakeTourismRecommendationService`

외부 API를 확실히 끄려면:

```properties
APP_EXTERNAL_API_ENABLED=false
```

## Frontend Keys

프론트엔드에는 공개 가능한 값만 둡니다.

```properties
VITE_API_BASE_URL=http://localhost:8080
VITE_KAKAO_JAVASCRIPT_KEY=
```

Kakao JavaScript 키를 사용할 경우 Kakao Developers에서 허용 도메인을 반드시 제한합니다.
