package com.tour_diary;

import com.tour_diary.dog.domain.DogProfile;
import com.tour_diary.dog.repository.DogProfileRepository;
import com.tour_diary.walk.domain.WalkRecord;
import com.tour_diary.walk.repository.WalkRecordRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.demo.seed", havingValue = "true")
public class DemoDataSeeder implements CommandLineRunner {

    private final DogProfileRepository dogProfileRepository;
    private final WalkRecordRepository walkRecordRepository;

    public DemoDataSeeder(
            DogProfileRepository dogProfileRepository,
            WalkRecordRepository walkRecordRepository
    ) {
        this.dogProfileRepository = dogProfileRepository;
        this.walkRecordRepository = walkRecordRepository;
    }

    @Override
    public void run(String... args) {
        try {
            if (!dogProfileRepository.existsById("1")) {
                dogProfileRepository.save(new DogProfile(
                        "1",
                        "Bori",
                        "Curious, gentle, and easily excited by new smells",
                        List.of("parks", "snacks", "fallen leaves"),
                        "Warm and playful first-person dog voice",
                        4
                ));
            }

            if (!walkRecordRepository.existsById("3")) {
                walkRecordRepository.save(new WalkRecord(
                        "3",
                        "1",
                        null,
                        37.5444,
                        127.0374,
                        "Seoul Forest, Seongdong-gu, Seoul",
                        "clear",
                        19.5,
                        Instant.now()
                ));
            }
        } catch (DataAccessException ex) {
            System.err.println("Demo data seed skipped because MongoDB is unavailable: " + ex.getMessage());
        }
    }
}
