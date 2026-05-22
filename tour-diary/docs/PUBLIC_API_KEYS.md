# Public API Keys

## Required

### KTO_TOUR_API_KEY

- Provider: data.go.kr
- Service: 한국관광공사_국문 관광정보서비스_GW
- Usage: Nearby tourism candidates from the walk location
- Current endpoint: `KorService2.locationBasedList2`

### KTO_PET_TOUR_API_KEY

- Provider: data.go.kr
- Service: 한국관광공사_반려동물 동반여행 서비스
- Usage: Pet-friendly travel candidates and pet-related notes
- Current endpoint: `KorPetTourService2.locationBasedList2`

## Recommended

### KAKAO_REST_API_KEY

- Provider: Kakao Developers
- Usage: Address/coordinate lookup and local place search fallback

## Optional

### KTO_DURUNUBI_API_KEY

- Provider: data.go.kr
- Service: 한국관광공사_두루누비 정보 서비스
- Usage: Walking trail recommendations

### KTO_ACCESSIBLE_TOUR_API_KEY

- Provider: data.go.kr
- Service: 한국관광공사_무장애 여행 정보
- Usage: Senior dogs, stroller-friendly routes, and accessibility-aware recommendations

### KTO_GREEN_TOUR_API_KEY

- Provider: data.go.kr
- Service: 한국관광공사_생태 관광 정보_GW
- Usage: Nature and ecology-focused recommendations

## .env Template

```properties
KTO_TOUR_API_KEY=
KTO_TOUR_API_BASE_URL=https://apis.data.go.kr/B551011/KorService2

KTO_PET_TOUR_API_KEY=
KTO_PET_TOUR_API_BASE_URL=https://apis.data.go.kr/B551011/KorPetTourService2

KTO_DURUNUBI_API_KEY=
KTO_ACCESSIBLE_TOUR_API_KEY=
KTO_GREEN_TOUR_API_KEY=

KAKAO_REST_API_KEY=
```
