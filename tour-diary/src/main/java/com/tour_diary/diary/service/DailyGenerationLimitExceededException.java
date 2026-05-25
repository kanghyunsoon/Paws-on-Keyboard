package com.tour_diary.diary.service;

public class DailyGenerationLimitExceededException extends RuntimeException {

    public DailyGenerationLimitExceededException(int dailyLimit) {
        super("오늘 생성 가능 횟수 " + dailyLimit + "회를 모두 사용했습니다. 내일 다시 생성해 주세요.");
    }
}
