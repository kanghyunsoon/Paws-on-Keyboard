package com.tour_diary.diary.controller;

import com.tour_diary.auth.service.AuthUserResolver;
import com.tour_diary.diary.controller.dto.GenerateDiaryRequest;
import com.tour_diary.diary.controller.dto.GenerateDiaryResponse;
import com.tour_diary.diary.controller.dto.DiaryDebugResponse;
import com.tour_diary.diary.service.DailyGenerationLimitExceededException;
import com.tour_diary.diary.service.DiaryGenerationService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/diaries", produces = "application/json; charset=UTF-8")
public class DiaryController {

    private final DiaryGenerationService diaryGenerationService;
    private final AuthUserResolver authUserResolver;

    public DiaryController(DiaryGenerationService diaryGenerationService, AuthUserResolver authUserResolver) {
        this.diaryGenerationService = diaryGenerationService;
        this.authUserResolver = authUserResolver;
    }

    @PostMapping("/generate")
    public GenerateDiaryResponse generate(
            @RequestBody GenerateDiaryRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return diaryGenerationService.generate(request, authUserResolver.optionalUserId(authorization));
    }

    @GetMapping("/{diaryId}")
    public GenerateDiaryResponse getDiary(@PathVariable String diaryId) {
        return diaryGenerationService.getDiary(diaryId);
    }

    @GetMapping
    public List<GenerateDiaryResponse> listDiaries(
            @RequestParam(value = "dogId", defaultValue = "current-dog") String dogId,
            @RequestParam(value = "userId", required = false) String userId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        String authenticatedUserId = authUserResolver.optionalUserId(authorization);
        if (authenticatedUserId != null && !authenticatedUserId.isBlank()) {
            return diaryGenerationService.listDiariesByUser(authenticatedUserId);
        }
        if (userId != null && !userId.isBlank()) {
            return diaryGenerationService.listDiariesByUser(userId);
        }
        return diaryGenerationService.listDiaries(dogId);
    }

    @GetMapping("/{diaryId}/debug")
    public DiaryDebugResponse getDiaryDebug(@PathVariable String diaryId) {
        return diaryGenerationService.getDiaryDebug(diaryId);
    }

    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    @org.springframework.web.bind.annotation.ExceptionHandler(DailyGenerationLimitExceededException.class)
    public String generationLimitExceeded(DailyGenerationLimitExceededException ex) {
        return ex.getMessage();
    }
}
