package com.tour_diary.diary.service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DailyGenerationLimiter {

    private final int dailyLimit;
    private final Map<String, Usage> usageByUserId = new HashMap<>();

    public DailyGenerationLimiter(@Value("${app.generation.daily-limit:3}") int dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public synchronized void acquire(String userId) {
        if (dailyLimit <= 0) {
            return;
        }

        LocalDate today = LocalDate.now();
        String key = userId == null || userId.isBlank() ? "local-user" : userId;
        Usage usage = usageByUserId.get(key);
        if (usage == null || !today.equals(usage.date())) {
            usage = new Usage(today, 0);
        }

        if (usage.count() >= dailyLimit) {
            throw new DailyGenerationLimitExceededException(dailyLimit);
        }

        usageByUserId.put(key, new Usage(today, usage.count() + 1));
    }

    private record Usage(LocalDate date, int count) {
    }
}
