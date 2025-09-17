package org.benefitmap.backend.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.benefitmap.backend.user.*;
import org.springframework.beans.factory.annotation.Value;
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

@Component
@RequiredArgsConstructor
@Transactional
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;

    @Value("${app.oauth2.redirect:http://localhost:3000/oauth2/callback}")
    private String redirectUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        if (!(authentication instanceof OAuth2AuthenticationToken oauth2)) {
            response.sendError(401, "Not OAuth2 auth");
            return;
        }

        OAuth2User p = oauth2.getPrincipal();
        Map<String,Object> at = p.getAttributes();

        String provider = oauth2.getAuthorizedClientRegistrationId(); // "google"
        String sub      = Objects.toString(at.get("sub"), null);
        String email    = Objects.toString(at.get("email"), null);
        String name     = Objects.toString(at.get("name"), "");
        String picture  = Objects.toString(at.get("picture"), null);

        if (sub == null || email == null) {
            response.sendError(401, "OAuth2 user info missing 'sub' or 'email'");
            return;
        }

        User user = userRepository.findByProviderAndProviderId(provider, sub)
                .orElseGet(() -> userRepository.findByEmail(email).orElse(null));

        if (user == null) {
            user = User.builder()
                    .provider(provider)
                    .providerId(sub)
                    .email(email)
                    .name(name)
                    .imageUrl(picture)
                    .role(Role.ROLE_USER)
                    .status(UserStatus.PENDING)
                    .build();
        } else {
            user.setProvider(provider);
            user.setProviderId(sub);
            user.setName(name);
            user.setImageUrl(picture);
        }
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String accessToken  = jwtProvider.createAccessToken(user.getId(), user.getRole().name());
        String refreshToken = jwtProvider.createRefreshToken(user.getId(), user.getRole().name());

        String tokenHash = sha256Hex(refreshToken);
        refreshTokenRepository.save(
                RefreshToken.builder()
                        .user(user)
                        .tokenHash(tokenHash)
                        .expiresAt(Instant.now().plusSeconds(jwtProvider.getRefreshTtlSeconds()))
                        .build()
        );

        Cookie atc = new Cookie("ACCESS_TOKEN", accessToken);
        atc.setHttpOnly(true); atc.setPath("/"); atc.setMaxAge((int) jwtProvider.getAccessTtlSeconds());
        Cookie rtc = new Cookie("REFRESH_TOKEN", refreshToken);
        rtc.setHttpOnly(true); rtc.setPath("/"); rtc.setMaxAge((int) jwtProvider.getRefreshTtlSeconds());
        response.addCookie(atc);
        response.addCookie(rtc);

        response.sendRedirect(redirectUrl);
    }

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
