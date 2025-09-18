package org.benefitmap.backend.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.benefitmap.backend.user.Role;
import org.benefitmap.backend.user.User;
import org.benefitmap.backend.user.UserRepository;
import org.benefitmap.backend.user.UserStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

/**
 * JWT 인증 필터
 *
 * 변경 핵심(4단계):
 * - JWT에서는 사용자 ID(sub)만 신뢰
 * - 권한(Role)과 상태(Status)는 항상 DB에서 최신값을 조회
 * - 상태가 ACTIVE인 경우에만 인증 성공 처리
 *
 * 효과:
 * - 관리자가 권한을 바꾸면, 이전에 발급된 토큰이라도 DB 최신 권한이 즉시 반영됨
 * - 강등/정지(PENDING, SUSPENDED 등) 시 즉시 차단 가능
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 인증이 불필요한 경로(로그인/OAuth2/문서/리프레시)는 스킵
        String p = request.getRequestURI();
        return p.startsWith("/login")
                || p.startsWith("/oauth2")
                || p.startsWith("/swagger")
                || p.startsWith("/v3")
                || p.equals("/auth/refresh");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        // 통상 Authorization: Bearer <token>
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                // 1) 토큰 파싱 (서명/만료 검증 포함)
                Jws<Claims> jws = jwtProvider.parse(token);

                // 2) 토큰에선 userId(sub)만 추출하고 role은 쓰지 않음
                String sub = jws.getPayload().getSubject();
                if (sub != null) {
                    Long userId = Long.valueOf(sub);

                    // 3) DB에서 최신 사용자 로드 (권한 + 상태 확인)
                    Optional<User> opt = userRepository.findById(userId);
                    if (opt.isPresent()) {
                        User user = opt.get();

                        // 상태가 ACTIVE가 아니면 인증하지 않음
                        if (user.getStatus() == UserStatus.ACTIVE) {
                            // DB의 최신 Role로 권한 구성
                            Role role = user.getRole(); // ROLE_USER / ROLE_ADMIN ...
                            var authorities = Collections.singletonList(
                                    new SimpleGrantedAuthority(role.name())
                            );

                            // 4) 인증 토큰 생성 (principal은 userId만 보관)
                            var auth = new UsernamePasswordAuthenticationToken(
                                    userId, null, authorities
                            );
                            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                            SecurityContextHolder.getContext().setAuthentication(auth);
                        }
                    }
                }
            } catch (JwtException | IllegalArgumentException ignore) {
                // 유효하지 않은 토큰(서명 오류/만료/포맷 불량 등)은 무시하고 다음 필터 진행
                // (추후 401 처리를 원하면 여기서 response.setStatus(401) 등으로 정책 변경 가능)
            }
        }

        chain.doFilter(request, response);
    }
}
