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
                        "사진 속 강아지",
                        "사진 속 상황을 궁금해하고 보호자를 자주 올려다보는 성격",
                        List.of("보호자와 산책하기", "사진 속 장소 냄새 맡기"),
                        "사진에 없는 사물이나 색을 지어내지 않는 강아지 1인칭 말투",
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
                        "사진 속 산책 장소",
                        "사진 속 날씨",
                        19.5,
                        Instant.now()
                ));
            }
        } catch (DataAccessException ex) {
            System.err.println("Demo data seed skipped because MongoDB is unavailable: " + ex.getMessage());
        }
    }
}
