package com.tour_diary.diary.controller;

import com.tour_diary.diary.controller.dto.GenerateDiaryRequest;
import com.tour_diary.diary.controller.dto.GenerateDiaryResponse;
import com.tour_diary.diary.controller.dto.DiaryDebugResponse;
import com.tour_diary.diary.service.DiaryGenerationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/diaries")
public class DiaryController {

    private final DiaryGenerationService diaryGenerationService;

    public DiaryController(DiaryGenerationService diaryGenerationService) {
        this.diaryGenerationService = diaryGenerationService;
    }

    @PostMapping("/generate")
    public GenerateDiaryResponse generate(@RequestBody GenerateDiaryRequest request) {
        return diaryGenerationService.generate(request);
    }

    @GetMapping("/{diaryId}")
    public GenerateDiaryResponse getDiary(@PathVariable String diaryId) {
        return diaryGenerationService.getDiary(diaryId);
    }

    @GetMapping("/{diaryId}/debug")
    public DiaryDebugResponse getDiaryDebug(@PathVariable String diaryId) {
        return diaryGenerationService.getDiaryDebug(diaryId);
    }
}
