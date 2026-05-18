package com.tour_diary.diary.service;

import com.tour_diary.ai.image.AiImageService;
import com.tour_diary.ai.text.AiTextService;
import com.tour_diary.ai.text.DiaryTextResult;
import com.tour_diary.ai.vision.VisionAnalysisResult;
import com.tour_diary.ai.vision.VisionService;
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
import java.util.List;
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

    public DiaryGenerationService(
            DogProfileRepository dogProfileRepository,
            WalkRecordRepository walkRecordRepository,
            DiaryRepository diaryRepository,
            VisionService visionService,
            DiaryPromptBuilder diaryPromptBuilder,
            ImagePromptBuilder imagePromptBuilder,
            AiTextService aiTextService,
            AiImageService aiImageService,
            TourismRecommendationService tourismRecommendationService
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
    }

    public GenerateDiaryResponse generate(GenerateDiaryRequest request) {
        DogProfile dog = dogProfileRepository.findById(request.dogId())
                .orElseThrow(() -> new IllegalArgumentException("Dog profile not found: " + request.dogId()));
        WalkRecord walk = walkRecordRepository.findById(request.walkRecordId())
                .orElseThrow(() -> new IllegalArgumentException("Walk record not found: " + request.walkRecordId()));

        VisionAnalysisResult vision = visionService.analyze(walk.originalImageUrl());
        String diaryPrompt = diaryPromptBuilder.build(dog, walk, vision);
        DiaryTextResult diaryText = aiTextService.generateDiary(diaryPrompt);
        String imagePrompt = imagePromptBuilder.build(dog, walk, vision, diaryText.content());
        String generatedImageUrl = aiImageService.generateImage(imagePrompt);
        List<RecommendedPlace> places = tourismRecommendationService.recommend(dog, walk, vision, diaryText.emotion());

        Diary diary = diaryRepository.save(new Diary(
                null,
                walk.id(),
                dog.id(),
                diaryText.title(),
                diaryText.content(),
                diaryText.emotion(),
                vision.objects(),
                diaryPrompt,
                imagePrompt,
                generatedImageUrl,
                places,
                Instant.now()
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

    private GenerateDiaryResponse toResponse(Diary diary, String originalImageUrl) {
        return new GenerateDiaryResponse(
                diary.id(),
                originalImageUrl,
                diary.generatedImageUrl(),
                diary.title(),
                diary.content(),
                diary.detectedObjects(),
                diary.recommendedPlaces()
        );
    }
}
