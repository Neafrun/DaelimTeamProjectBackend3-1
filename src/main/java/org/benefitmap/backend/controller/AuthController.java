package org.benefitmap.backend.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.benefitmap.backend.auth.RefreshToken;
import org.benefitmap.backend.auth.RefreshTokenRepository;
import org.benefitmap.backend.auth.JwtProvider;
import org.benefitmap.backend.user.User;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest req, HttpServletResponse res) {
        String refresh = readCookie(req, "REFRESH_TOKEN");
        if (refresh == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing refresh");

        String hash = sha256Hex(refresh);
        Optional<RefreshToken> opt = refreshTokenRepository.findByTokenHash(hash);
        if (opt.isEmpty() || opt.get().getExpiresAt().isBefore(Instant.now())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired");
        }

        User user = opt.get().getUser();
        String newAccess = jwtProvider.createAccessToken(user.getId(), user.getRole().name());
        Cookie atc = new Cookie("ACCESS_TOKEN", newAccess);
        atc.setHttpOnly(true); atc.setPath("/"); atc.setMaxAge((int) jwtProvider.getAccessTtlSeconds());
        res.addCookie(atc);

        return ResponseEntity.ok(Map.of("ok", true));
    }

    private static String readCookie(HttpServletRequest req, String name) {
        Cookie[] cookies = req.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) if (name.equals(c.getName())) return c.getValue();
        return null;
    }

    private static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
}
