package org.benefitmap.backend.controller;

import lombok.RequiredArgsConstructor;
import org.benefitmap.backend.auth.RefreshTokenRepository;
import org.benefitmap.backend.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;               // Swagger 문서 태그
import io.swagger.v3.oas.annotations.Operation;              // 엔드포인트 설명
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.time.Duration;

/**
 * 사용자 관련 컨트롤러
 * - DELETE /user/me : 회원 탈퇴 (계정 삭제 + Refresh 토큰 제거 + 쿠키 즉시 만료)
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Tag(name = "User", description = "사용자 계정 API")
public class UserController {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;

    @Operation(
            summary = "회원 탈퇴",
            description = "로그인한 사용자의 계정을 삭제하고 모든 리프레시 토큰을 제거합니다. ACCESS/REFRESH 쿠키를 즉시 만료합니다.",
            security = @SecurityRequirement(name = "cookieAuth"),
            responses = {
                    @ApiResponse(responseCode = "204", description = "탈퇴 완료"),
                    @ApiResponse(responseCode = "401", description = "인증 필요")
            }
    )
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMe(@AuthenticationPrincipal Long userId) {
        // 1) 해당 사용자의 모든 Refresh 토큰 삭제
        refreshTokenRepository.deleteByUser_Id(userId);
        // 2) 사용자 계정 삭제 (과제 범위: 실제 삭제. 필요하면 status=DELETED 로 변경해도 됨)
        userRepository.deleteById(userId);

        // 3) 두 쿠키 즉시 만료(Set-Cookie)
        ResponseCookie expiredAccess  = expiredCookie("ACCESS_TOKEN");
        ResponseCookie expiredRefresh = expiredCookie("REFRESH_TOKEN");

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expiredAccess.toString())
                .header(HttpHeaders.SET_COOKIE, expiredRefresh.toString())
                .build();
    }

    /** SameSite=None / Secure / HttpOnly 로 즉시 만료 쿠키 생성 */
    private ResponseCookie expiredCookie(String name) {
        return ResponseCookie.from(name, "")
                .httpOnly(true).secure(cookieSecure).sameSite("None")
                .path("/").maxAge(Duration.ZERO).build();
    }
}
