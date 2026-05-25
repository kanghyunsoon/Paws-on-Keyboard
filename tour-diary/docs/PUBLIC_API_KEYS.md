# Public API Keys

This project uses public-data and map provider keys on the backend only. Do not put server-side keys in the React app.

## Required For Full External Mode

### KTO_TOUR_API_KEY

- Provider: data.go.kr
- Service: 한국관광공사 국문 관광정보 서비스 GW
- Usage: nearby tourism candidates from the walk location
- Current endpoint: `KorService2.locationBasedList2`

### KTO_PET_TOUR_API_KEY

- Provider: data.go.kr
- Service: 한국관광공사 반려동물 동반여행 서비스
- Usage: pet-friendly travel candidates and pet-related notes
- Current endpoint: `KorPetTourService2.locationBasedList2`

## Recommended

### KAKAO_REST_API_KEY

- Provider: Kakao Developers
- Usage: address/coordinate lookup and local place search fallback

## Optional

### KTO_DURUNUBI_API_KEY

- Provider: data.go.kr
- Service: 한국관광공사 두루누비 정보 서비스
- Usage: walking trail recommendations

### KTO_ACCESSIBLE_TOUR_API_KEY

- Provider: data.go.kr
- Service: 한국관광공사 무장애 여행 정보
- Usage: senior dogs, stroller-friendly routes, and accessibility-aware recommendations

### KTO_GREEN_TOUR_API_KEY

- Provider: data.go.kr
- Service: 한국관광공사 생태 관광 정보 GW
- Usage: nature and ecology-focused recommendations

## Environment Template

```properties
APP_EXTERNAL_API_ENABLED=true

KTO_TOUR_API_KEY=
KTO_TOUR_API_BASE_URL=https://apis.data.go.kr/B551011/KorService2

KTO_PET_TOUR_API_KEY=
KTO_PET_TOUR_API_BASE_URL=https://apis.data.go.kr/B551011/KorPetTourService2

KTO_DURUNUBI_API_KEY=
KTO_ACCESSIBLE_TOUR_API_KEY=
KTO_GREEN_TOUR_API_KEY=

KAKAO_REST_API_KEY=
```

For offline demos, set:

```properties
APP_EXTERNAL_API_ENABLED=false
```
