package com.example.junction2025.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 설정 클래스
 * CORS 설정 및 기타 웹 관련 설정
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")  // /api/** 경로에 CORS 적용
                .allowedOriginPatterns("*")  // 모든 오리진 허용 (개발 환경)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);  // pre-flight 요청 캐시 시간 (1시간)
    }
    
    // AuthInterceptor가 필요한 경우 아래 코드를 주석 해제하세요
//    private final AuthInterceptor authInterceptor;
//    
//    public WebConfig(AuthInterceptor authInterceptor) {
//        this.authInterceptor = authInterceptor;
//    }
//
//    // 인증이 필요하지 않은 경로 목록
//    private static final List<String> EXCLUDE_PATHS = Arrays.asList(
//        "/api/auth/**",           // 인증 관련 API
//        "/api/health/**",         // 헬스 체크
//        "/api/support",           // 서포터 API
//        "/api/supporters",        // 서포터 조회 API
//        "/swagger-ui/**",         // Swagger UI
//        "/v3/api-docs/**",        // Swagger API 문서
//        "/error"                  // 에러 페이지
//    );
//
//    @Override
//    public void addInterceptors(InterceptorRegistry registry) {
//        registry.addInterceptor(authInterceptor)
//                .addPathPatterns("/api/**")  // /api/** 경로에 Interceptor 적용
//                .excludePathPatterns(EXCLUDE_PATHS);  // 제외 경로 설정
//    }
}

