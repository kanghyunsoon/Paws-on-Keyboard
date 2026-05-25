package com.tour_diary.profile.controller;

import com.tour_diary.auth.service.AuthUserResolver;
import com.tour_diary.dog.domain.DogProfile;
import com.tour_diary.dog.repository.DogProfileRepository;
import com.tour_diary.profile.controller.dto.SaveProfileRequest;
import com.tour_diary.profile.domain.UserProfile;
import com.tour_diary.profile.repository.UserProfileRepository;
import java.util.List;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/profile", produces = "application/json; charset=UTF-8")
public class ProfileController {

    private static final String LOCAL_USER_ID = "local-user";
    private static final String LOCAL_DOG_ID = "current-dog";

    private final UserProfileRepository userProfileRepository;
    private final DogProfileRepository dogProfileRepository;
    private final AuthUserResolver authUserResolver;

    public ProfileController(
            UserProfileRepository userProfileRepository,
            DogProfileRepository dogProfileRepository,
            AuthUserResolver authUserResolver
    ) {
        this.userProfileRepository = userProfileRepository;
        this.dogProfileRepository = dogProfileRepository;
        this.authUserResolver = authUserResolver;
    }

    @GetMapping("/current")
    public UserProfile getCurrentProfile(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return findProfile(effectiveUserId(authorization, LOCAL_USER_ID));
    }

    @GetMapping("/{userId}")
    public UserProfile getProfile(@PathVariable String userId) {
        return findProfile(textOrDefault(userId, LOCAL_USER_ID));
    }

    private UserProfile findProfile(String userId) {
        String cleanUserId = textOrDefault(userId, LOCAL_USER_ID);
        try {
            return userProfileRepository.findById(cleanUserId)
                    .orElseGet(() -> emptyProfile(cleanUserId));
        } catch (DataAccessException ex) {
            return emptyProfile(cleanUserId);
        }
    }

    @PutMapping("/current")
    public UserProfile saveCurrentProfile(
            @RequestBody SaveProfileRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return saveProfileForUser(effectiveUserId(authorization, textOrDefault(request.userId(), LOCAL_USER_ID)), request);
    }

    @PutMapping("/{userId}")
    public UserProfile saveProfile(
            @PathVariable String userId,
            @RequestBody SaveProfileRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return saveProfileForUser(effectiveUserId(authorization, userId), request);
    }

    private UserProfile saveProfileForUser(String userId, SaveProfileRequest request) {
        String cleanUserId = textOrDefault(userId, LOCAL_USER_ID);
        String dogId = dogIdFor(cleanUserId);
        UserProfile profile = new UserProfile(
                cleanUserId,
                text(request.userName()),
                dogId,
                text(request.dogName()),
                request.dogAge(),
                text(request.dogBreed()),
                text(request.dogAppearance()),
                text(request.dogFavoriteThings()),
                text(request.dogDislikedThings()),
                text(request.dogTraits()),
                text(request.ownerName()),
                textOrDefault(request.ownerRole(), "형아"),
                text(request.ownerNickname()),
                text(request.ownerGender()),
                text(request.relationshipNote()),
                text(request.dogPhotoUrl()),
                text(request.ownerPhotoUrl())
        );

        DogProfile dog = new DogProfile(
                dogId,
                textOrDefault(profile.dogName(), "강아지"),
                textOrDefault(profile.dogTraits(), "사진 속 순간을 좋아하는 강아지"),
                List.of(
                        "사진 속 산책",
                        textOrDefault(profile.dogBreed(), "강아지"),
                        textOrDefault(profile.dogFavoriteThings(), "보호자와 함께 있기")
                ),
                buildSpeakingStyle(profile),
                profile.dogAge()
        );

        try {
            dogProfileRepository.save(dog);
            return userProfileRepository.save(profile);
        } catch (DataAccessException ex) {
            return profile;
        }
    }

    private String effectiveUserId(String authorization, String fallbackUserId) {
        return textOrDefault(authUserResolver.optionalUserId(authorization), fallbackUserId);
    }

    private UserProfile emptyProfile(String userId) {
        return new UserProfile(
                userId,
                "",
                dogIdFor(userId),
                "",
                null,
                "",
                "",
                "",
                "",
                "",
                "",
                "형아",
                "",
                "",
                "",
                null,
                null
        );
    }

    private String dogIdFor(String userId) {
        if (LOCAL_USER_ID.equals(userId)) {
            return LOCAL_DOG_ID;
        }
        return userId + "-dog";
    }

    private String buildSpeakingStyle(UserProfile profile) {
        return """
                강아지 1인칭 그림일기 말투.
                보호자는 %s라고 부른다.
                보호자 이름/별명: %s
                강아지를 부르는 애칭: %s
                보호자 성별/특징: %s
                관계: %s
                견종/외형: %s / %s
                좋아하는 것: %s
                싫어하는 것: %s
                """.formatted(
                textOrDefault(profile.ownerRole(), "형아"),
                textOrDefault(profile.ownerName(), profile.ownerRole()),
                textOrDefault(profile.ownerNickname(), "이름으로 부름"),
                textOrDefault(profile.ownerGender(), "초기 세팅 기준"),
                textOrDefault(profile.relationshipNote(), "보호자를 좋아함"),
                textOrDefault(profile.dogBreed(), "사진 기준"),
                textOrDefault(profile.dogAppearance(), "사진 기준"),
                textOrDefault(profile.dogFavoriteThings(), "사진 속 산책"),
                textOrDefault(profile.dogDislikedThings(), "없음")
        );
    }

    private String text(String value) {
        return value == null ? "" : value.trim();
    }

    private String textOrDefault(String value, String fallback) {
        String clean = text(value);
        return clean.isBlank() ? fallback : clean;
    }
}
