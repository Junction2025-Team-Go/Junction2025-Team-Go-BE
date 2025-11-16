package com.example.junction2025.utils;

import jakarta.servlet.http.HttpServletRequest;

public class AuthUtils {

    /**
     * HttpServletRequest에서 현재 인증된 사용자의 ID를 가져옵니다.
     * AuthInterceptor에서 설정한 userId 속성을 사용합니다.
     *
     * @param request HttpServletRequest
     * @return 사용자 ID
     * @throws IllegalStateException 인증되지 않은 요청인 경우
     */
    public static Long getCurrentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId == null) {
            throw new IllegalStateException("인증되지 않은 요청입니다.");
        }
        return (Long) userId;
    }
}
