package com.tour_diary.diary.service;

import com.tour_diary.ai.image.AiImageService;
import com.tour_diary.ai.text.AiTextService;
import com.tour_diary.ai.text.DiaryTextResult;
import com.tour_diary.ai.vision.VisionAnalysisResult;
import com.tour_diary.ai.vision.VisionService;
import com.tour_diary.diary.controller.dto.DiaryDebugResponse;
import com.tour_diary.diary.controller.dto.GenerateDiaryRequest;
import com.tour_diary.diary.controller.dto.GenerateDiaryResponse;
import com.tour_diary.diary.domain.Diary;
import com.tour_diary.diary.repository.DiaryRepository;
import com.tour_diary.dog.domain.DogProfile;
import com.tour_diary.dog.repository.DogProfileRepository;
import com.tour_diary.prompt.DiaryPromptBuilder;
import com.tour_diary.prompt.ImagePromptBuilder;
import com.tour_diary.tourism.RecommendedPlace;
import com.tour_diary.tourism.TourismRecommendationService;
import com.tour_diary.walk.domain.WalkRecord;
import com.tour_diary.walk.repository.WalkRecordRepository;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
public class DiaryGenerationService {

    private final DogProfileRepository dogProfileRepository;
    private final WalkRecordRepository walkRecordRepository;
    private final DiaryRepository diaryRepository;
    private final VisionService visionService;
    private final DiaryPromptBuilder diaryPromptBuilder;
    private final ImagePromptBuilder imagePromptBuilder;
    private final AiTextService aiTextService;
    private final AiImageService aiImageService;
    private final TourismRecommendationService tourismRecommendationService;
    private final DailyGenerationLimiter dailyGenerationLimiter;

    public DiaryGenerationService(
            DogProfileRepository dogProfileRepository,
            WalkRecordRepository walkRecordRepository,
            DiaryRepository diaryRepository,
            VisionService visionService,
            DiaryPromptBuilder diaryPromptBuilder,
            ImagePromptBuilder imagePromptBuilder,
            AiTextService aiTextService,
            AiImageService aiImageService,
            TourismRecommendationService tourismRecommendationService,
            DailyGenerationLimiter dailyGenerationLimiter
    ) {
        this.dogProfileRepository = dogProfileRepository;
        this.walkRecordRepository = walkRecordRepository;
        this.diaryRepository = diaryRepository;
        this.visionService = visionService;
        this.diaryPromptBuilder = diaryPromptBuilder;
        this.imagePromptBuilder = imagePromptBuilder;
        this.aiTextService = aiTextService;
        this.aiImageService = aiImageService;
        this.tourismRecommendationService = tourismRecommendationService;
        this.dailyGenerationLimiter = dailyGenerationLimiter;
    }

    public GenerateDiaryResponse generate(GenerateDiaryRequest request, String authenticatedUserId) {
        String effectiveUserId = textOrDefault(authenticatedUserId, textOrDefault(request.userId(), "local-user"));
        dailyGenerationLimiter.acquire(effectiveUserId);

        DogProfile dog = applyRequestProfile(resolveDog(request.dogId()), request);
        WalkRecord walk = applyRequestWalk(resolveWalk(request.walkRecordId(), dog.id()), request);

        VisionAnalysisResult vision = visionService.analyze(walk.originalImageUrl());
        String diaryPrompt = diaryPromptBuilder.build(dog, walk, vision);
        DiaryTextResult diaryText = aiTextService.generateDiary(diaryPrompt);
        String imagePrompt = imagePromptBuilder.build(dog, walk, vision, diaryText.content());
        String generatedImageUrl = aiImageService.generateImage(
                imagePrompt,
                diaryText.title(),
                diaryText.content(),
                walk.originalImageUrl(),
                request.dogPhotoUrl()
        );
        List<RecommendedPlace> places = tourismRecommendationService.recommend(dog, walk, vision, diaryText.emotion());
        String visionAnalysis = buildVisionAnalysis(vision);
        String tourismPrompt = buildTourismPrompt(dog, walk, vision, diaryText, places);
        String rawTourismResponse = buildRawTourismResponse(places);

        Instant createdAt = Instant.now();
        Diary diary = saveOrPreview(new Diary(
                null,
                effectiveUserId,
                walk.id(),
                dog.id(),
                diaryText.title(),
                diaryText.content(),
                diaryText.emotion(),
                vision.objects(),
                visionAnalysis,
                diaryPrompt,
                imagePrompt,
                tourismPrompt,
                rawTourismResponse,
                generatedImageUrl,
                places,
                createdAt
        ));

        return toResponse(diary, walk.originalImageUrl());
    }

    public GenerateDiaryResponse getDiary(String diaryId) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new IllegalArgumentException("Diary not found: " + diaryId));
        String originalImageUrl = walkRecordRepository.findById(diary.walkRecordId())
                .map(WalkRecord::originalImageUrl)
                .orElse(null);
        return toResponse(diary, originalImageUrl);
    }

    public List<GenerateDiaryResponse> listDiaries(String dogId) {
        try {
            return diaryRepository.findByDogIdOrderByCreatedAtDesc(textOrDefault(dogId, "current-dog"))
                    .stream()
                    .map(diary -> toResponse(
                            diary,
                            walkRecordRepository.findById(diary.walkRecordId())
                                    .map(WalkRecord::originalImageUrl)
                                    .orElse(null)
                    ))
                    .toList();
        } catch (DataAccessException ex) {
            return List.of();
        }
    }

    public List<GenerateDiaryResponse> listDiariesByUser(String userId) {
        try {
            return diaryRepository.findByUserIdOrderByCreatedAtDesc(textOrDefault(userId, "local-user"))
                    .stream()
                    .map(diary -> toResponse(
                            diary,
                            walkRecordRepository.findById(diary.walkRecordId())
                                    .map(WalkRecord::originalImageUrl)
                                    .orElse(null)
                    ))
                    .toList();
        } catch (DataAccessException ex) {
            return List.of();
        }
    }

    public DiaryDebugResponse getDiaryDebug(String diaryId) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new IllegalArgumentException("Diary not found: " + diaryId));

        return new DiaryDebugResponse(
                diary.visionAnalysis(),
                diary.diaryPrompt(),
                diary.imagePrompt(),
                diary.tourismPrompt(),
                diary.rawTourismResponse()
        );
    }

    private GenerateDiaryResponse toResponse(Diary diary, String originalImageUrl) {
        return new GenerateDiaryResponse(
                diary.id(),
                diary.userId(),
                diary.dogId(),
                diary.walkRecordId(),
                originalImageUrl,
                diary.generatedImageUrl(),
                diary.title(),
                diary.content(),
                diary.detectedObjects(),
                diary.recommendedPlaces(),
                diary.createdAt()
        );
    }

    private DogProfile resolveDog(String dogId) {
        try {
            return dogProfileRepository.findById(dogId)
                    .orElseGet(() -> demoDog(dogId));
        } catch (DataAccessException ex) {
            return demoDog(dogId);
        }
    }

    private WalkRecord resolveWalk(String walkRecordId, String dogId) {
        try {
            return walkRecordRepository.findById(walkRecordId)
                    .orElseGet(() -> demoWalk(walkRecordId, dogId));
        } catch (DataAccessException ex) {
            return demoWalk(walkRecordId, dogId);
        }
    }

    private Diary saveOrPreview(Diary diary) {
        try {
            return diaryRepository.save(diary);
        } catch (DataAccessException ex) {
            return new Diary(
                    "preview-" + diary.createdAt().toEpochMilli(),
                    diary.userId(),
                    diary.walkRecordId(),
                    diary.dogId(),
                    diary.title(),
                    diary.content(),
                    diary.emotion(),
                    diary.detectedObjects(),
                    diary.visionAnalysis(),
                    diary.diaryPrompt(),
                    diary.imagePrompt(),
                    diary.tourismPrompt(),
                    diary.rawTourismResponse(),
                    diary.generatedImageUrl(),
                    diary.recommendedPlaces(),
                    diary.createdAt()
            );
        }
    }

    private DogProfile demoDog(String dogId) {
        return new DogProfile(
                dogId == null || dogId.isBlank() ? "1" : dogId,
                "사진 속 강아지",
                "사진 속 상황을 궁금해하고 보호자를 자주 올려다보는 성격",
                List.of("보호자와 산책하기", "사진 속 장소 냄새 맡기"),
                "사진에 없는 사물이나 색을 지어내지 않는 강아지 1인칭 말투",
                4
        );
    }

    private DogProfile applyRequestProfile(DogProfile dog, GenerateDiaryRequest request) {
        String dogName = textOrDefault(request.dogName(), dog.name());
        Integer dogAge = request.dogAge() == null ? dog.age() : request.dogAge();
        String ownerRole = textOrDefault(request.ownerRole(), "보호자");
        String ownerName = textOrDefault(request.ownerName(), ownerRole);
        String breed = textOrDefault(request.dogBreed(), "");
        String appearance = textOrDefault(request.dogGender(), "");
        String personality = textOrDefault(request.dogPersonality(), dog.personality());
        String relationship = textOrDefault(request.relationshipNote(), "");
        String tone = textOrDefault(request.diaryTone(), "밝고 귀여운 강아지 1인칭 말투");
        String diaryNotes = textOrDefault(request.diaryPromptNotes(), "");
        String imageNotes = textOrDefault(request.imagePromptNotes(), "");
        String speakingStyle = """
                %s
                보호자는 %s라고 부른다.
                견종/외모: %s / %s
                초기 설정 강아지 사진: %s
                보호자와의 관계: %s
                일기 추가 지시: %s
                그림 추가 지시: %s
                """.formatted(tone, ownerName, breed, appearance, hasText(request.dogPhotoUrl()) ? "등록됨" : "없음", relationship, diaryNotes, imageNotes);

        return new DogProfile(
                dog.id(),
                dogName,
                personality,
                mergeFavoriteThings(dog.favoriteThings(), request.favoriteThings(), request.dislikedThings()),
                speakingStyle,
                dogAge
        );
    }

    private WalkRecord applyRequestWalk(WalkRecord walk, GenerateDiaryRequest request) {
        String location = textOrDefault(request.walkLocation(), walk.address());
        String weather = textOrDefault(request.walkWeather(), walk.weather());
        String activity = textOrDefault(request.walkActivity(), "");
        String address = activity.isBlank() ? location : location + " / 오늘 한 일: " + activity;

        return new WalkRecord(
                walk.id(),
                walk.dogId(),
                walk.originalImageUrl(),
                walk.latitude(),
                walk.longitude(),
                address,
                weather,
                walk.temperature(),
                walk.walkedAt()
        );
    }

    private List<String> mergeFavoriteThings(List<String> current, String favoriteThings, String dislikedThings) {
        String merged = "좋아하는 것: " + textOrDefault(favoriteThings, String.join(", ", current))
                + " / 싫어하는 것: " + textOrDefault(dislikedThings, "없음");
        return Arrays.stream(merged.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    private String textOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private WalkRecord demoWalk(String walkRecordId, String dogId) {
        return new WalkRecord(
                walkRecordId == null || walkRecordId.isBlank() ? "3" : walkRecordId,
                dogId,
                null,
                null,
                null,
                "사진 속 산책 장소",
                "사진 속 날씨",
                null,
                Instant.now()
        );
    }

    private String buildVisionAnalysis(VisionAnalysisResult vision) {
        return """
                {
                  "dogExists": %s,
                  "objects": %s,
                  "colors": %s,
                  "mood": "%s",
                  "placeType": "%s",
                  "stickerCandidates": %s,
                  "diaryHints": %s,
                  "sceneSummary": "%s",
                  "dogAppearance": "%s",
                  "ownerClue": "%s",
                  "dogAction": "%s",
                  "dogViewpoint": "%s",
                  "drawingKeywords": %s
                }
                """.formatted(
                vision.dogExists(),
                vision.objects(),
                vision.colors(),
                vision.mood(),
                vision.placeType(),
                vision.stickerCandidates(),
                vision.diaryHints(),
                vision.sceneSummary(),
                vision.dogAppearance(),
                vision.ownerClue(),
                vision.dogAction(),
                vision.dogViewpoint(),
                vision.drawingKeywords()
        );
    }

    private String buildTourismPrompt(
            DogProfile dog,
            WalkRecord walk,
            VisionAnalysisResult vision,
            DiaryTextResult diaryText,
            List<RecommendedPlace> places
    ) {
        return """
                너는 반려견 동반 여행 큐레이터다.

                [목표]
                한국관광공사 OpenAPI 후보지 중에서 오늘 산책 사진, 강아지 성향, 감정에 맞는 다음 산책/여행지를 추천한다.

                [강아지 정보]
                이름: %s
                성격: %s
                좋아하는 것: %s
                나이: %s

                [오늘 산책]
                위치: %s
                날씨: %s
                감정: %s
                사진 분위기: %s
                장소 유형: %s
                감지 객체: %s

                [KTO 후보 데이터]
                %s

                [작성 규칙]
                - 추천 이유에는 반려견 동반 적합성을 반드시 포함한다.
                - 한국관광공사 데이터에 없는 사실은 단정하지 않는다.
                - 강아지 시점의 그림일기와 자연스럽게 이어지도록 작성한다.
                """.formatted(
                dog.name(),
                dog.personality(),
                dog.favoriteThings(),
                dog.age(),
                walk.address(),
                walk.weather(),
                diaryText.emotion(),
                vision.mood(),
                vision.placeType(),
                vision.objects(),
                buildRawTourismResponse(places)
        );
    }

    private String buildRawTourismResponse(List<RecommendedPlace> places) {
        return places.stream()
                .map(place -> """
                        - name: %s
                          category: %s
                          address: %s
                          sourceProvider: %s
                          sourceApi: %s
                          sourceContentId: %s
                          petInfo: %s
                          distanceMeters: %s
                        """.formatted(
                        place.name(),
                        place.category(),
                        place.address(),
                        place.sourceProvider(),
                        place.sourceApi(),
                        place.sourceContentId(),
                        place.petInfo(),
                        place.distanceMeters()
                ))
                .toList()
                .toString();
    }
}
