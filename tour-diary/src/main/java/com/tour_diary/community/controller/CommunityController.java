package com.tour_diary.community.controller;

import com.tour_diary.auth.service.AuthUserResolver;
import com.tour_diary.community.controller.dto.CreateCommentRequest;
import com.tour_diary.community.controller.dto.CreatePostRequest;
import com.tour_diary.community.controller.dto.BadgeResponse;
import com.tour_diary.community.controller.dto.FollowSummaryResponse;
import com.tour_diary.community.controller.dto.ToggleFollowRequest;
import com.tour_diary.community.controller.dto.ToggleLikeRequest;
import com.tour_diary.community.domain.CommunityComment;
import com.tour_diary.community.domain.CommunityPost;
import com.tour_diary.community.domain.FollowRelation;
import com.tour_diary.community.repository.CommunityPostRepository;
import com.tour_diary.community.repository.FollowRelationRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/community", produces = "application/json; charset=UTF-8")
public class CommunityController {

    private final CommunityPostRepository postRepository;
    private final FollowRelationRepository followRepository;
    private final AuthUserResolver authUserResolver;

    public CommunityController(
            CommunityPostRepository postRepository,
            FollowRelationRepository followRepository,
            AuthUserResolver authUserResolver
    ) {
        this.postRepository = postRepository;
        this.followRepository = followRepository;
        this.authUserResolver = authUserResolver;
    }

    @GetMapping("/posts")
    public List<CommunityPost> listPosts(
            @RequestParam(value = "sort", defaultValue = "latest") String sort,
            @RequestParam(value = "query", defaultValue = "") String query
    ) {
        try {
            List<CommunityPost> posts = postRepository.findAllByOrderByCreatedAtDesc();
            String keyword = text(query);
            if (!keyword.isBlank()) {
                posts = posts.stream()
                        .filter(post -> (post.title() + " " + post.content() + " " + post.dogName() + " " + post.place()).contains(keyword))
                        .toList();
            }
            if ("popular".equals(sort)) {
                return posts.stream()
                        .sorted(Comparator.comparingInt((CommunityPost post) -> post.likes().size()).reversed())
                        .toList();
            }
            return posts;
        } catch (DataAccessException ex) {
            return List.of();
        }
    }

    @GetMapping("/leaderboard")
    public List<CommunityPost> leaderboard(
            @RequestParam(value = "period", defaultValue = "weekly") String period,
            @RequestParam(value = "limit", defaultValue = "10") int limit
    ) {
        try {
            return postRepository.findAllByOrderByCreatedAtDesc()
                    .stream()
                    .filter(post -> isInPeriod(post.createdAt(), period))
                    .sorted(Comparator.comparingInt((CommunityPost post) -> post.likes().size()).reversed())
                    .limit(Math.max(1, Math.min(limit, 50)))
                    .toList();
        } catch (DataAccessException ex) {
            return List.of();
        }
    }

    @GetMapping("/badges")
    public List<BadgeResponse> badges(
            @RequestParam(value = "userId", required = false) String userId,
            @RequestParam(value = "period", defaultValue = "monthly") String period,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        String effectiveUserId = effectiveUserId(authorization, userId);
        List<CommunityPost> ranked = leaderboard(period, 20);
        List<BadgeResponse> badges = new ArrayList<>();
        for (int index = 0; index < ranked.size(); index++) {
            CommunityPost post = ranked.get(index);
            if (post.authorId().equals(effectiveUserId)) {
                badges.add(new BadgeResponse(
                        effectiveUserId,
                        post.id(),
                        post.dogName() + " " + (index + 1) + "위 추억 배지",
                        buildBadgePrompt(post, index + 1, period),
                        post.dogPhotoPreview(),
                        post.ownerPhotoPreview(),
                        index + 1,
                        period,
                        Instant.now()
                ));
            }
        }
        return badges;
    }

    @PostMapping("/posts")
    public CommunityPost createPost(
            @RequestBody CreatePostRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        String authorId = effectiveUserId(authorization, request.authorId());
        CommunityPost post = new CommunityPost(
                null,
                authorId,
                text(request.authorName()),
                text(request.dogName()),
                text(request.dogPhotoPreview()),
                text(request.ownerPhotoPreview()),
                text(request.diaryId()),
                text(request.title()),
                text(request.content()),
                text(request.imagePreview()),
                text(request.place()),
                Instant.now(),
                List.of(),
                List.of()
        );
        return postRepository.save(post);
    }

    @PostMapping("/posts/{postId}/like")
    public CommunityPost toggleLike(
            @PathVariable String postId,
            @RequestBody ToggleLikeRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        CommunityPost post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found: " + postId));
        String userId = effectiveUserId(authorization, request.userId());
        List<String> likes = new ArrayList<>(post.likes());
        if (likes.contains(userId)) {
            likes.remove(userId);
        } else if (!userId.isBlank()) {
            likes.add(userId);
        }
        return postRepository.save(copy(post, likes, post.comments()));
    }

    @PostMapping("/posts/{postId}/comments")
    public CommunityPost addComment(
            @PathVariable String postId,
            @RequestBody CreateCommentRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        CommunityPost post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found: " + postId));
        String authorId = effectiveUserId(authorization, request.authorId());
        List<CommunityComment> comments = new ArrayList<>(post.comments());
        comments.add(new CommunityComment(
                UUID.randomUUID().toString(),
                authorId,
                text(request.authorName()),
                text(request.content()),
                Instant.now()
        ));
        return postRepository.save(copy(post, post.likes(), comments));
    }

    @PostMapping("/follow")
    public FollowSummaryResponse toggleFollow(
            @RequestBody ToggleFollowRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        String followerId = effectiveUserId(authorization, request.followerId());
        String followingId = text(request.followingId());
        if (!followerId.isBlank() && !followingId.isBlank() && !followerId.equals(followingId)) {
            followRepository.findByFollowerIdAndFollowingId(followerId, followingId)
                    .ifPresentOrElse(
                            followRepository::delete,
                            () -> followRepository.save(new FollowRelation(null, followerId, followingId, Instant.now()))
                    );
        }
        return followSummaryForUser(followerId);
    }

    @GetMapping("/follows/{userId}")
    public FollowSummaryResponse followSummary(
            @PathVariable String userId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return followSummaryForUser(effectiveUserId(authorization, userId));
    }

    @GetMapping("/follows/current")
    public FollowSummaryResponse currentFollowSummary(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return followSummaryForUser(effectiveUserId(authorization, ""));
    }

    private FollowSummaryResponse followSummaryForUser(String userId) {
        try {
            return new FollowSummaryResponse(
                    followRepository.findByFollowerId(userId).stream().map(FollowRelation::followingId).toList(),
                    followRepository.findByFollowingId(userId).stream().map(FollowRelation::followerId).toList()
            );
        } catch (DataAccessException ex) {
            return new FollowSummaryResponse(List.of(), List.of());
        }
    }

    private CommunityPost copy(CommunityPost post, List<String> likes, List<CommunityComment> comments) {
        return new CommunityPost(
                post.id(),
                post.authorId(),
                post.authorName(),
                post.dogName(),
                post.dogPhotoPreview(),
                post.ownerPhotoPreview(),
                post.diaryId(),
                post.title(),
                post.content(),
                post.imagePreview(),
                post.place(),
                post.createdAt(),
                likes,
                comments
        );
    }

    private boolean isInPeriod(Instant createdAt, String period) {
        Instant now = Instant.now();
        long seconds = "monthly".equals(period) ? 31L * 24 * 60 * 60 : 7L * 24 * 60 * 60;
        return createdAt != null && createdAt.isAfter(now.minusSeconds(seconds));
    }

    private String buildBadgePrompt(CommunityPost post, int rank, String period) {
        return """
                반려견 커뮤니티 인기 게시물 배지 이미지.
                기간: %s
                순위: %s
                강아지 이름: %s
                보호자/작성자: %s
                원본 강아지 사진과 보호자 사진의 특징을 유지하되, 기존 배지와 다른 구도와 장식으로 만든다.
                따뜻한 산책 추억, 관광 공모전 발표용으로 보여주기 좋은 완성도.
                """.formatted(period, rank, post.dogName(), post.authorName());
    }

    private String text(String value) {
        return value == null ? "" : value.trim();
    }

    private String effectiveUserId(String authorization, String fallbackUserId) {
        String authUserId = authUserResolver.optionalUserId(authorization);
        return text(authUserId).isBlank() ? text(fallbackUserId) : text(authUserId);
    }
}
