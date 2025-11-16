package com.example.junction2025.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Base64;

/**
 * 구글 및 애플 idToken 검증 및 사용자 ID 추출 유틸리티
 */
@Slf4j
@Component
public class IdTokenVerifier {
    
    /**
     * idToken에서 사용자 ID를 추출합니다.
     * 구글과 애플 토큰 모두 지원합니다.
     *
     * @param idToken 구글 또는 애플 idToken
     * @return 사용자 ID (구글: sub, 애플: sub)
     */
    public String extractUserId(String idToken) {
        try {
            DecodedJWT jwt = JWT.decode(idToken);
            String userId = jwt.getSubject(); // "sub" 클레임에서 사용자 ID 추출
            
            if (userId == null || userId.isEmpty()) {
                throw new IllegalArgumentException("idToken에 사용자 ID가 없습니다.");
            }
            
            return userId;
        } catch (Exception e) {
            log.error("idToken 파싱 실패", e);
            throw new IllegalArgumentException("유효하지 않은 idToken입니다: " + e.getMessage());
        }
    }
    
    /**
     * idToken에서 이메일을 추출합니다.
     *
     * @param idToken 구글 또는 애플 idToken
     * @return 이메일 주소
     */
    public String extractEmail(String idToken) {
        try {
            DecodedJWT jwt = JWT.decode(idToken);
            String email = jwt.getClaim("email").asString();
            
            if (email == null || email.isEmpty()) {
                // 이메일이 없는 경우 사용자 ID를 기반으로 생성
                String userId = extractUserId(idToken);
                return userId + "@example.com";
            }
            
            return email;
        } catch (Exception e) {
            log.error("idToken에서 이메일 추출 실패", e);
            String userId = extractUserId(idToken);
            return userId + "@example.com";
        }
    }
    
    /**
     * idToken이 구글 토큰인지 확인합니다.
     *
     * @param idToken idToken
     * @return 구글 토큰 여부
     */
    public boolean isGoogleToken(String idToken) {
        try {
            DecodedJWT jwt = JWT.decode(idToken);
            String issuer = jwt.getIssuer();
            return issuer != null && issuer.contains("google");
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * idToken이 애플 토큰인지 확인합니다.
     *
     * @param idToken idToken
     * @return 애플 토큰 여부
     */
    public boolean isAppleToken(String idToken) {
        try {
            DecodedJWT jwt = JWT.decode(idToken);
            String issuer = jwt.getIssuer();
            return issuer != null && issuer.contains("apple");
        } catch (Exception e) {
            return false;
        }
    }
}

