package com.tour_diary.auth.controller;

import com.tour_diary.auth.controller.dto.AuthRequest;
import com.tour_diary.auth.controller.dto.AuthResponse;
import com.tour_diary.auth.domain.UserAccount;
import com.tour_diary.auth.repository.UserAccountRepository;
import com.tour_diary.auth.service.AuthTokenService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/auth", produces = "application/json; charset=UTF-8")
public class AuthController {

    private final UserAccountRepository userAccountRepository;
    private final AuthTokenService authTokenService;

    public AuthController(UserAccountRepository userAccountRepository, AuthTokenService authTokenService) {
        this.userAccountRepository = userAccountRepository;
        this.authTokenService = authTokenService;
    }

    @PostMapping("/signup")
    public AuthResponse signup(@RequestBody AuthRequest request) {
        String email = normalizeEmail(request.email());
        String name = text(request.name());
        String password = text(request.password());
        if (email.isBlank() || name.isBlank() || password.isBlank()) {
            throw new BadAuthRequestException("이메일, 이름, 비밀번호를 입력해 주세요.");
        }

        try {
            if (userAccountRepository.findByEmail(email).isPresent()) {
                throw new BadAuthRequestException("이미 가입된 이메일입니다.");
            }
            UserAccount saved = userAccountRepository.save(new UserAccount(
                    null,
                    email,
                    name,
                    hash(password),
                    Instant.now()
            ));
            return toResponse(saved);
        } catch (DataAccessException ex) {
            throw new BadAuthRequestException("회원가입 저장소에 연결할 수 없습니다.");
        }
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest request) {
        String email = normalizeEmail(request.email());
        String password = text(request.password());
        if (email.isBlank() || password.isBlank()) {
            throw new BadAuthRequestException("이메일과 비밀번호를 입력해 주세요.");
        }

        try {
            UserAccount user = userAccountRepository.findByEmail(email)
                    .orElseThrow(() -> new BadAuthRequestException("가입 정보가 없거나 비밀번호가 다릅니다."));
            if (!user.passwordHash().equals(hash(password))) {
                throw new BadAuthRequestException("가입 정보가 없거나 비밀번호가 다릅니다.");
            }
            return toResponse(user);
        } catch (DataAccessException ex) {
            throw new BadAuthRequestException("로그인 저장소에 연결할 수 없습니다.");
        }
    }

    @GetMapping("/me")
    public AuthResponse me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String userId;
        try {
            userId = authTokenService.verifyAndGetUserId(bearerToken(authorization));
        } catch (IllegalArgumentException ex) {
            throw new BadAuthRequestException("로그인이 필요합니다.");
        }
        try {
            UserAccount user = userAccountRepository.findById(userId)
                    .orElseThrow(() -> new BadAuthRequestException("사용자를 찾을 수 없습니다."));
            return toResponse(user);
        } catch (DataAccessException ex) {
            throw new BadAuthRequestException("로그인 저장소에 연결할 수 없습니다.");
        }
    }

    private AuthResponse toResponse(UserAccount user) {
        AuthTokenService.TokenIssue token = authTokenService.issue(user.id());
        return new AuthResponse(user.id(), user.email(), user.name(), token.token(), token.expiresAt().toString());
    }

    private String bearerToken(String authorization) {
        String clean = text(authorization);
        if (!clean.startsWith("Bearer ")) {
            throw new BadAuthRequestException("로그인이 필요합니다.");
        }
        return clean.substring("Bearer ".length()).trim();
    }

    private String normalizeEmail(String value) {
        return text(value).toLowerCase(Locale.ROOT);
    }

    private String text(String value) {
        return value == null ? "" : value.trim();
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    private static class BadAuthRequestException extends RuntimeException {
        BadAuthRequestException(String message) {
            super(message);
        }
    }
}
