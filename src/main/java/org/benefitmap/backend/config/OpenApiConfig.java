package org.benefitmap.backend.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/OpenAPI 기본 설정
 * - cookieAuth: ACCESS_TOKEN 쿠키를 인증 수단으로 사용
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "Benefit Map API", version = "v1", description = "Google OIDC + JWT(쿠키) 기반 API"),
        servers = {
                @Server(url = "http://localhost:8080", description = "Local")
        }
)
@SecurityScheme(
        name = "cookieAuth",                 // ↓ @SecurityRequirement 에서 이 이름으로 참조
        type = SecuritySchemeType.APIKEY,    // APIKEY + COOKIE 조합으로 쿠키 인증 표현
        in = SecuritySchemeIn.COOKIE,
        paramName = "ACCESS_TOKEN"           // 쿠키 이름
)
public class OpenApiConfig {}
