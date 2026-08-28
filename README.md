# Paws on Keyboard

<p align="center">
  <img src="img/title.png" width="620" alt="Paws on Keyboard - Tour Diary" />
</p>

<p align="center">
  산책 사진 한 장을 강아지 시점의 그림일기와 다음 산책 후보로 바꾸는 AI 에이전트 서비스
</p>

<p align="center">
  <a href="docs/portfolio-ai-orchestration.md">AI 오케스트레이션 기록</a> ·
  <a href="https://github.com/kanghyunsoon/HF_ProxyAPI">Hugging Face 프록시</a>
</p>

> 2026 관광데이터 활용 공모전 · 생성형 AI 활용 관광 프롬프트 부문
>
> 팀 프로젝트 · 강형순 담당: **AI 에이전트 오케스트레이션, 외부 API 파이프라인, 프록시 서버 배포, 프롬프트 설계**

## 이 저장소가 담는 것

서비스는 Ennoia 에이전트 툴에서 구현했다. Ennoia가 완성된 애플리케이션 코드를 내려받는 기능을 제공하지 않아, 이 저장소는 일반적인 원본 소스 저장소가 아니다.

대신 면접에서 확인할 수 있도록 다음 자료를 남겼다.

- 실제 서비스 화면과 생성 결과
- `Vision → Planner → Writer → Painter` 에이전트 전달 구조
- 한국관광공사 데이터 4종을 추천 근거로 연결한 방식
- Ennoia와 Hugging Face 사이의 규격 차이를 해결한 프록시 코드

![담당 범위 수치](docs/assets/portfolio/key-metrics.svg)

## 맡은 일

| 영역 | 담당 내용 | 사용 기술 |
| --- | --- | --- |
| AI 오케스트레이션 | 사진 분석, 관광지 탐색, 일기 작성, 이미지 생성 역할 분리와 전달 순서 설계 | Ennoia, prompt engineering |
| 외부 API 파이프라인 | 반려동물 동반·무장애·두루누비·TourAPI 데이터를 추천 근거로 연결 | 한국관광공사 OpenAPI |
| 이미지 생성 연결 | Ennoia 요청을 Hugging Face Gradio 규격으로 바꾸는 프록시 설계·배포 | FastAPI, Python, Gradio Client |
| 프롬프트 조정 | 사진 단서, 강아지 프로필, 추천 근거가 다음 에이전트에서 빠지지 않도록 출력 규칙 고정 | structured prompt, role prompt |

## 1. 한 프롬프트에 섞여 있던 역할을 네 단계로 나눴다

**문제**

사진 분석, 관광지 추천, 일기 작성, 이미지 생성을 한 번에 처리하면 어느 단계에서 결과가 달라졌는지 찾기 어려웠다. 이미지가 실사나 종이 사진처럼 생성되거나, 추천 이유가 빠지거나, 화면에 HTML 조각이 노출되는 문제가 함께 발생했다.

**해결**

에이전트마다 입력과 출력 책임을 하나씩 부여했다.

```mermaid
flowchart LR
    U[산책 사진·강아지 프로필] --> V[Vision<br/>강아지·풍경 단서]
    V --> P[Planner<br/>관광데이터 후보]
    P --> W[Writer<br/>강아지 1인칭 일기]
    W --> I[Painter<br/>그림 생성 프롬프트]
    I --> R[그림일기·추천 카드]
```

- Vision은 사진에서 강아지, 배경, 행동 단서만 추출한다.
- Planner는 공공데이터 후보와 추천 근거를 만든다.
- Writer는 강아지 프로필과 사진 단서로 1인칭 일기를 쓴다.
- Painter는 앞 단계 결과를 그림 생성용 프롬프트로 바꾼다.

**결과**

결과 화면을 `사진 기반 일러스트`, `강아지 말투의 2문장 일기`, `근거가 표시된 추천 카드` 세 영역으로 고정했다. 어느 단계의 출력이 화면에 반영되는지도 분리해서 확인할 수 있게 됐다.

## 2. Ennoia가 만들 수 없는 Hugging Face 요청을 프록시로 분리했다

**문제**

Ennoia 에이전트 툴에서는 Hugging Face Gradio Space가 요구하는 요청 배열을 코드로 조립하고, 응답에 포함된 파일 경로나 중첩 객체를 다시 해석할 수 없었다. 프롬프트는 만들어도 이미지 URL을 서비스 화면까지 전달하지 못했다.

**해결**

FastAPI 프록시를 별도 서버로 배포했다. Ennoia는 프롬프트만 JSON으로 보내고, 프록시는 Gradio 호출과 결과 해석을 담당한다.

![Ennoia와 Hugging Face 사이의 프록시 흐름](docs/assets/portfolio/proxy-flow.svg)

프록시는 두 요청 형태를 받는다.

```json
{"prompt":"강아지 시점의 크레용 그림일기","seed":1234}
```

```json
{"data":["강아지 시점의 크레용 그림일기",768,1024,8,1234,false]}
```

Hugging Face 응답이 URL, 로컬 파일, 배열, 중첩 객체 중 어떤 형태로 와도 이미지 값을 찾고, Ennoia가 사용할 `image_url`을 반환하도록 정리했다. 구현은 [HF_ProxyAPI](https://github.com/kanghyunsoon/HF_ProxyAPI)에서 확인할 수 있다.

## 3. 추천 결과에 공공데이터 근거를 남겼다

**문제**

장소 이름만 생성하면 사용자는 추천 이유를 확인할 수 없다. 반려견 동반 여부나 이동 편의 정보가 없는 추천은 실제 산책 후보를 고르는 데 쓰기 어렵다.

**해결**

Planner가 네 종류의 한국관광공사 데이터를 목적별로 조회하도록 구성했다.

| 데이터 | 사용한 근거 |
| --- | --- |
| 반려동물 동반 여행 서비스 | 반려동물 동반 가능 시설과 산책 후보 |
| 무장애 관광정보 API | 노견, 펫 유모차, 보호자 이동 편의 |
| 두루누비 걷기길 데이터 | 산책로와 걷기 후보 |
| 국문 관광정보 TourAPI | 관광지 명칭, 위치, 상세 정보 |

추천 카드에는 장소명만 표시하지 않고 데이터 출처와 선택 이유를 함께 노출했다.

## 결과 화면

<table>
  <tr>
    <td width="50%" align="center"><img src="img/render.png" alt="강아지 시점 그림일기" /></td>
    <td width="50%" align="center"><img src="img/recommend.png" alt="근거가 표시된 추천 코스 카드" /></td>
  </tr>
  <tr>
    <td align="center">사진 단서와 프로필을 반영한 그림일기</td>
    <td align="center">공공데이터 근거를 표시한 추천 카드</td>
  </tr>
</table>

## 기술 선택

| 선택 | 이유 |
| --- | --- |
| Ennoia | 에이전트 역할과 전달 순서를 구성하고 서비스 화면까지 연결하기 위해 사용 |
| FastAPI 프록시 | 에이전트 툴에 없는 요청 변환과 응답 파싱을 서버 경계로 분리 |
| Hugging Face Gradio Space | 이미지 생성 모델을 HTTP 호출 대상으로 사용 |
| 한국관광공사 OpenAPI | 추천 문장에 확인 가능한 관광·이동 근거를 포함 |
| 구조화 프롬프트 | 에이전트별 출력 범위와 다음 단계 입력 형식을 고정 |

## 확인 가능한 범위

- 서비스 화면과 발표 자료로 에이전트 입력·출력 흐름을 확인할 수 있다.
- 프록시 저장소에서 Ennoia 요청 변환과 Hugging Face 응답 처리 코드를 확인할 수 있다.

## 현재 한계

- Ennoia 원본 애플리케이션 소스는 export할 수 없어 이 저장소만으로 제출 서비스를 재현할 수 없다.
- 모델 품질, 응답 시간, 사용자 평가를 같은 조건에서 측정한 기록은 없다.
- Hugging Face Space의 모델과 응답 형식이 바뀌면 프록시도 함께 수정해야 한다.
- 프록시는 결과 파일 만료와 재시도 정책까지 구현한 운영 서버가 아니라 공모전 연결을 위한 어댑터다.

## 코드와 설계 기록

- [AI 에이전트 오케스트레이션과 프록시 설계](docs/portfolio-ai-orchestration.md)
- [실제 프록시 구현](https://github.com/kanghyunsoon/HF_ProxyAPI/blob/main/main.py)
