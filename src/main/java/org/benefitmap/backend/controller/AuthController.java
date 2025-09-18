package org.benefitmap.backend.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.benefitmap.backend.auth.RefreshToken;
import org.benefitmap.backend.auth.RefreshTokenRepository;
import org.benefitmap.backend.auth.JwtProvider;
import org.benefitmap.backend.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;                // Swagger 문서 태그
import io.swagger.v3.oas.annotations.Operation;              // 엔드포인트 설명
import io.swagger.v3.oas.annotations.responses.ApiResponse;  // 응답 문서
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * 인증/인가 관련 컨트롤러
 * - /auth/refresh : 리프레시 토큰으로 액세스 토큰 재발급
 * - /auth/logout  : 로그아웃(Refresh 토큰 무효화 + 쿠키 즉시 만료)
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "로그인/토큰/로그아웃 API")
public class AuthController {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;

    // 로컬(http) 테스트 시 false 로 두면 브라우저에 쿠키가 보임, 실서버(https)는 true 권장
    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;

    /**
     * /auth/refresh
     * - REFRESH_TOKEN 쿠키가 유효하면 ACCESS_TOKEN 재발급
     * - 응답은 ResponseCookie로 내려 SameSite/Secure/HttpOnly 등을 명확히 설정
     */
    @Operation(
            summary = "액세스 토큰 재발급",
            description = "브라우저의 REFRESH_TOKEN 쿠키를 검증하여 ACCESS_TOKEN을 새로 발급합니다. 응답으로 ACCESS_TOKEN 쿠키가 내려옵니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "재발급 성공",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = RefreshOk.class))),
                    @ApiResponse(responseCode = "401", description = "리프레시 토큰 없음/만료/무효", content = @Content)
            }
    )
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest req) {
        // 1) 요청 쿠키에서 refresh 추출
        String refresh = readCookie(req, "REFRESH_TOKEN");
        if (refresh == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing refresh");
        }

        // 2) 해시로 DB 조회 + 만료 확인
        String hash = sha256Hex(refresh);
        Optional<RefreshToken> opt = refreshTokenRepository.findByTokenHash(hash);
        if (opt.isEmpty() || opt.get().getExpiresAt().isBefore(Instant.now())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired");
        }

        // 3) ACCESS_TOKEN 재발급
        User user = opt.get().getUser();
        String newAccess = jwtProvider.createAccessToken(user.getId(), user.getRole().name());

        // 4) ResponseCookie로 보안 속성 강화하여 쿠키 전송
        ResponseCookie atCookie = ResponseCookie.from("ACCESS_TOKEN", newAccess)
                .httpOnly(true)                               // JS 접근 차단
                .secure(cookieSecure)                         // HTTPS에서만
                .sameSite("None")                             // 크로스 도메인 허용
                .path("/")                                    // 전 경로
                .maxAge(jwtProvider.getAccessTtlSeconds())    // Access 만료와 동일
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, atCookie.toString())
                .body(new RefreshOk(true));
    }

    /**
     * 로그아웃
     * - 현재 기기의 REFRESH_TOKEN 해시를 찾아 DB에서 해당 토큰 삭제
     * - 쿠키(ACCESS_TOKEN/REFRESH_TOKEN)를 즉시 만료(Set-Cookie)
     * - 쿠키가 없다면 사용자 전체 Refresh 토큰 삭제(폴백) — 과제 범위에서는 생략, 필요 시 확장
     */
    @Operation(
            summary = "로그아웃",
            description = "현재 기기의 REFRESH_TOKEN을 무효화하고, ACCESS_TOKEN/REFRESH_TOKEN 쿠키를 즉시 만료합니다.",
            security = @SecurityRequirement(name = "cookieAuth"),
            responses = {
                    @ApiResponse(responseCode = "204", description = "로그아웃 완료"),
                    @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content)
            }
    )
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest req) {
        String refresh = readCookie(req, "REFRESH_TOKEN");
        if (refresh != null && !refresh.isBlank()) {
            // 현재 기기의 refresh 토큰만 우선 삭제
            String hash = sha256Hex(refresh);
            refreshTokenRepository.deleteByTokenHash(hash);
        }

        // 즉시 만료 쿠키 내려서 브라우저에서 제거
        ResponseCookie expiredAccess  = expiredCookie("ACCESS_TOKEN");
        ResponseCookie expiredRefresh = expiredCookie("REFRESH_TOKEN");

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expiredAccess.toString())
                .header(HttpHeaders.SET_COOKIE, expiredRefresh.toString())
                .build();
    }

    // --- Swagger 문서용 간단 응답 DTO ---
    record RefreshOk(boolean ok) {}

    // ===== 내부 유틸 =====

    /** 요청 쿠키에서 특정 이름의 값을 읽음 */
    private static String readCookie(HttpServletRequest req, String name) {
        Cookie[] cookies = req.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) if (name.equals(c.getName())) return c.getValue();
        return null;
    }

    /** SHA-256 해시(Refresh 토큰 해시 저장과 동일 로직) */
    private static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    /** SameSite=None / Secure / HttpOnly 로 즉시 만료 쿠키 생성 */
    private ResponseCookie expiredCookie(String name) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("None")
                .path("/")
                .maxAge(0) // 즉시 만료
                .build();
    }
}
