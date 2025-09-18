package org.benefitmap.backend.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.benefitmap.backend.user.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

/**
 * OAuth2 로그인 성공 시 실행되는 핸들러
 *  1. 구글 사용자 정보를 DB에 등록/갱신
 *  2. Access / Refresh JWT 발급
 *  3. 보안 속성 강화된 쿠키로 브라우저에 전달
 */
@Component
@RequiredArgsConstructor
@Transactional
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;

    /** 로그인 성공 후 프론트엔드로 리다이렉트할 URL */
    @Value("${app.oauth2.redirect:http://localhost:3000/oauth2/callback}")
    private String redirectUrl;

    /** 로컬 테스트시 secure 쿠키를 끄고 싶을 때 false 로 설정 가능 */
    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        // 1) OAuth2 인증이 아닌 경우 401
        if (!(authentication instanceof OAuth2AuthenticationToken oauth2)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Not OAuth2 auth");
            return;
        }

        // 2) 구글 사용자 정보 추출
        OAuth2User principal = oauth2.getPrincipal();
        Map<String, Object> attr = principal.getAttributes();

        String provider = oauth2.getAuthorizedClientRegistrationId(); // 예: "google"
        String sub      = Objects.toString(attr.get("sub"), null);
        String email    = Objects.toString(attr.get("email"), null);
        String name     = Objects.toString(attr.get("name"), "");
        String picture  = Objects.toString(attr.get("picture"), null);

        if (sub == null || email == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                    "OAuth2 user info missing 'sub' or 'email'");
            return;
        }

        // 3) 기존 사용자 조회( provider+sub 또는 email )
        User user = userRepository.findByProviderAndProviderId(provider, sub)
                .orElseGet(() -> userRepository.findByEmail(email).orElse(null));

        // 4) 신규/기존 사용자 정보 저장
        if (user == null) {
            user = User.builder()
                    .provider(provider)
                    .providerId(sub)
                    .email(email)
                    .name(name)
                    .imageUrl(picture)
                    .role(Role.ROLE_USER)
                    .status(UserStatus.PENDING) // 최초 가입은 PENDING
                    .build();
        } else {
            user.setProvider(provider);
            user.setProviderId(sub);
            user.setName(name);
            user.setImageUrl(picture);
        }
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // 5) JWT 발급
        String accessToken  = jwtProvider.createAccessToken(user.getId(), user.getRole().name());
        String refreshToken = jwtProvider.createRefreshToken(user.getId(), user.getRole().name());

        // 6) Refresh 토큰 해시 저장(DB)
        String tokenHash = sha256Hex(refreshToken);
        refreshTokenRepository.save(
                RefreshToken.builder()
                        .user(user)
                        .tokenHash(tokenHash)
                        .expiresAt(Instant.now().plusSeconds(jwtProvider.getRefreshTtlSeconds()))
                        .build()
        );

        // 7) 보안 속성 강화된 쿠키 생성
        ResponseCookie accessCookie = ResponseCookie.from("ACCESS_TOKEN", accessToken)
                .httpOnly(true)                                 // JS 접근 차단
                .secure(cookieSecure)                           // HTTPS 전용(프로필별 제어)
                .sameSite("None")                               // 크로스 도메인 허용
                .path("/")                                      // 전체 경로
                .maxAge(jwtProvider.getAccessTtlSeconds())      // Access 만료와 동일
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("REFRESH_TOKEN", refreshToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("None")
                .path("/")
                .maxAge(jwtProvider.getRefreshTtlSeconds())     // Refresh 만료와 동일
                .build();

        // 8) 쿠키를 응답 헤더에 추가
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        // 9) 프론트엔드로 리다이렉트
        response.sendRedirect(redirectUrl);
    }

    /** Refresh 토큰 해시 저장용 헬퍼 */
    private static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
