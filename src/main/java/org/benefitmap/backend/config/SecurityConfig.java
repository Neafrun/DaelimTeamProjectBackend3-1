package org.benefitmap.backend.config;

import lombok.RequiredArgsConstructor;
import org.benefitmap.backend.auth.CustomOAuth2UserService;
import org.benefitmap.backend.auth.JwtAuthenticationFilter;
import org.benefitmap.backend.auth.OAuth2SuccessHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * 보안 설정
 * - JWT 중심의 무상태(stateless) 아키텍처
 * - OAuth2 로그인 성공 시 토큰 발급
 * - JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 배치
 * - 관리자/사용자 권한에 따른 접근 경로를 분리
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;          // 요청마다 JWT 인증 처리
    private final OAuth2SuccessHandler successHandler;        // OAuth2 성공 시 토큰/쿠키 처리
    private final CustomOAuth2UserService oAuth2UserService;  // 구글 사용자 정보 매핑

    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF는 세션/폼 기반이 아니므로 비활성화
                .csrf(csrf -> csrf.disable())

                // CORS 설정 적용
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 세션 완전 무상태: SecurityContext를 세션에 저장하지 않음
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 폼 로그인/베이직 인증 사용 안 함 (우리는 OAuth2 + JWT만 사용)
                .httpBasic(b -> b.disable())
                .formLogin(f -> f.disable())

                // 인가 규칙
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/hello", "/error").permitAll()

                        // Swagger/OpenAPI 전부 허용
                        .requestMatchers("/swagger-ui.html").permitAll()
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .requestMatchers("/swagger-resources/**", "/webjars/**").permitAll() // (호환용)

                        // 로그아웃은 인증 필요 (아래 /auth/** permitAll 보다 위에 둔다)
                        .requestMatchers("/auth/logout").authenticated()

                        // 로그인/리프레시 등
                        .requestMatchers("/auth/**").permitAll()

                        .requestMatchers(HttpMethod.GET, "/actuator/**").permitAll()  // 헬스체크/메트릭
                        .requestMatchers(
                                "/login/success",
                                "/oauth2/authorization/**",
                                "/login/oauth2/**"
                        ).permitAll()

                        // 관리자 전용 API: ROLE_ADMIN 권한이 있어야 접근 가능
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // 사용자 전용 API: USER 또는 ADMIN 권한이 있으면 접근 가능
                        .requestMatchers("/user/**").hasAnyRole("USER","ADMIN")

                        .anyRequest().authenticated()  // 나머지는 인증 필요
                )

                // OAuth2 로그인(세션은 쓰지 않지만, OAuth2 flow 자체는 유지)
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(ui -> ui.userService(oAuth2UserService))
                        .successHandler(successHandler)
                );

        // JWT 필터는 UsernamePasswordAuthenticationFilter 앞에 배치
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS 설정
     * - allowedOrigins: 환경변수/설정파일로 관리 (쉼표 구분)
     * - allowedHeaders: 현재는 *로 열어둠(차후 최소화 예정)
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration c = new CorsConfiguration();
        for (String o : allowedOrigins.split(",")) {
            c.addAllowedOrigin(o.trim());
        }
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("*"));
        c.setAllowCredentials(true); // 쿠키 기반 토큰 전달 허용
        UrlBasedCorsConfigurationSource s = new UrlBasedCorsConfigurationSource();
        s.registerCorsConfiguration("/**", c);
        return s;
    }
}
