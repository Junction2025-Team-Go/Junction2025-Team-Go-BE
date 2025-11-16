package com.example.junction2025.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * JWT 토큰 생성 및 검증 유틸리티
 */
@Slf4j
@Component
public class JwtTokenProvider {
    
    private static final long ACCESS_TOKEN_EXPIRATION_HOURS = 1; // 1시간
    private static final long REFRESH_TOKEN_EXPIRATION_DAYS = 30; // 30일
    
    private final Algorithm algorithm;
    private final String secretKey;
    
    public JwtTokenProvider(@Value("${jwt.secret:your-secret-key-change-in-production}") String secretKey) {
        this.secretKey = secretKey;
        this.algorithm = Algorithm.HMAC256(secretKey);
    }
    
    /**
     * Access Token을 생성합니다 (유효기간: 1시간)
     *
     * @param userId 사용자 ID
     * @return Access Token
     */
    public String generateAccessToken(Long userId) {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(ACCESS_TOKEN_EXPIRATION_HOURS * 3600);
        
        return JWT.create()
                .withSubject(String.valueOf(userId))
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(expiration))
                .withClaim("type", "access")
                .sign(algorithm);
    }
    
    /**
     * Refresh Token을 생성합니다 (유효기간: 30일)
     *
     * @param userId 사용자 ID
     * @return Refresh Token
     */
    public String generateRefreshToken(Long userId) {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(REFRESH_TOKEN_EXPIRATION_DAYS * 24 * 3600);
        
        return JWT.create()
                .withSubject(String.valueOf(userId))
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(expiration))
                .withClaim("type", "refresh")
                .sign(algorithm);
    }
    
    /**
     * Access Token의 만료 시간을 반환합니다.
     *
     * @return 만료 시간 (LocalDateTime)
     */
    public LocalDateTime getAccessTokenExpiration() {
        return LocalDateTime.now().plusHours(ACCESS_TOKEN_EXPIRATION_HOURS);
    }
    
    /**
     * Refresh Token의 만료 시간을 반환합니다.
     *
     * @return 만료 시간 (LocalDateTime)
     */
    public LocalDateTime getRefreshTokenExpiration() {
        return LocalDateTime.now().plusDays(REFRESH_TOKEN_EXPIRATION_DAYS);
    }
    
    /**
     * 토큰에서 사용자 ID를 추출합니다.
     *
     * @param token JWT 토큰
     * @return 사용자 ID
     */
    public Long extractUserId(String token) {
        try {
            String userId = JWT.decode(token).getSubject();
            return Long.parseLong(userId);
        } catch (Exception e) {
            log.error("토큰에서 사용자 ID 추출 실패", e);
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }
    }
    
    /**
     * 토큰의 유효성을 검증합니다.
     *
     * @param token JWT 토큰
     * @return 유효 여부
     */
    public boolean validateToken(String token) {
        try {
            JWT.require(algorithm).build().verify(token);
            return true;
        } catch (Exception e) {
            log.debug("토큰 검증 실패: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 토큰이 만료되었는지 확인합니다.
     *
     * @param token JWT 토큰
     * @return 만료 여부
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = JWT.decode(token).getExpiresAt();
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
}

