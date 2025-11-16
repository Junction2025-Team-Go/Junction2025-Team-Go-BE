//package com.example.junction2025.interceptor;
//
//import com.example.junction2025.utils.JwtTokenProvider;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//import org.springframework.web.servlet.HandlerInterceptor;
//
///**
// * Access Token 검증 Interceptor
// */
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class AuthInterceptor implements HandlerInterceptor {
//
//    private final JwtTokenProvider jwtTokenProvider;
//
//    @Override
//    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
//        // OPTIONS 요청은 통과
//        if ("OPTIONS".equals(request.getMethod())) {
//            return true;
//        }
//
//        // Authorization 헤더에서 토큰 추출
//        String authHeader = request.getHeader("Authorization");
//
//        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
//            log.warn("Authorization 헤더가 없거나 형식이 잘못되었습니다: {}", request.getRequestURI());
//            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//            return false;
//        }
//
//        String token = authHeader.substring(7); // "Bearer " 제거
//
//        // 토큰 검증
//        if (!jwtTokenProvider.validateToken(token)) {
//            log.warn("유효하지 않은 토큰: {}", request.getRequestURI());
//            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//            return false;
//        }
//
//        if (jwtTokenProvider.isTokenExpired(token)) {
//            log.warn("만료된 토큰: {}", request.getRequestURI());
//            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//            return false;
//        }
//
//        // 토큰에서 사용자 ID 추출하여 request에 저장
//        try {
//            Long userId = jwtTokenProvider.extractUserId(token);
//            request.setAttribute("userId", userId);
//            log.debug("인증 성공 - userId: {}, uri: {}", userId, request.getRequestURI());
//            return true;
//        } catch (Exception e) {
//            log.error("토큰에서 사용자 ID 추출 실패", e);
//            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//            return false;
//        }
//    }
//}
//
