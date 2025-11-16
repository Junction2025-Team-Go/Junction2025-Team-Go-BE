package com.example.junction2025.services;

import com.example.junction2025.domain.Token;
import com.example.junction2025.domain.User;
import com.example.junction2025.dto.response.TokenResponse;
import com.example.junction2025.repository.TokenRepository;
import com.example.junction2025.repository.UserRepository;
import com.example.junction2025.utils.IdTokenVerifier;
import com.example.junction2025.utils.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 인증 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final IdTokenVerifier idTokenVerifier;
    private final JwtTokenProvider jwtTokenProvider;
    
    /**
     * idToken으로 사용자를 등록하거나 로그인합니다.
     * 이미 존재하는 사용자라면 토큰을 갱신합니다.
     *
     * @param idToken 구글 또는 애플 idToken
     * @return Access Token과 Refresh Token
     */
    @Transactional
    public TokenResponse registerUser(String idToken) {
        // idToken에서 사용자 정보 추출
        String userId = idTokenVerifier.extractUserId(idToken);
        String email = idTokenVerifier.extractEmail(idToken);
        
        log.info("사용자 등록/로그인 시도 - userId: {}, email: {}", userId, email);
        
        // 기존 사용자 확인
        Optional<User> existingUser = userRepository.findByUserId(userId);
        User user;
        
        if (existingUser.isPresent()) {
            // 기존 사용자 - 토큰 갱신
            user = existingUser.get();
            log.info("기존 사용자 로그인 - userId: {}", userId);
        } else {
            // 신규 사용자 - 생성
            user = User.builder()
                    .userId(userId)
                    .email(email)
                    .build();
            user = userRepository.save(user);
            log.info("신규 사용자 등록 - userId: {}, email: {}", userId, email);
        }
        
        // 토큰 생성
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());
        LocalDateTime refreshTokenExpiresAt = jwtTokenProvider.getRefreshTokenExpiration();
        
        // 기존 토큰 확인 및 업데이트 또는 생성
        Optional<Token> existingToken = tokenRepository.findByUserId(user.getId());
        Token token;
        
        if (existingToken.isPresent()) {
            token = existingToken.get();
            token.updateRefreshToken(refreshToken, refreshTokenExpiresAt);
        } else {
            token = Token.builder()
                    .user(user)
                    .refreshToken(refreshToken)
                    .expiresAt(refreshTokenExpiresAt)
                    .build();
        }
        
        tokenRepository.save(token);
        
        log.info("토큰 발급 완료 - userId: {}", user.getId());
        
        return new TokenResponse(accessToken, refreshToken);
    }
    
    /**
     * Refresh Token으로 새로운 Access Token과 Refresh Token을 발급합니다.
     *
     * @param refreshToken Refresh Token
     * @return 새로운 Access Token과 Refresh Token
     */
    @Transactional
    public TokenResponse refreshUser(String refreshToken) {
        log.info("토큰 갱신 시도");
        
        // Refresh Token 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 Refresh Token입니다.");
        }
        
        if (jwtTokenProvider.isTokenExpired(refreshToken)) {
            throw new IllegalArgumentException("만료된 Refresh Token입니다.");
        }
        
        // Refresh Token에서 사용자 ID 추출
        Long userId = jwtTokenProvider.extractUserId(refreshToken);
        
        // DB에서 Refresh Token 확인
        Token token = tokenRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 Refresh Token입니다."));
        
        if (token.isExpired()) {
            throw new IllegalArgumentException("만료된 Refresh Token입니다.");
        }
        
        // 사용자 확인
        User user = token.getUser();
        if (!user.getId().equals(userId)) {
            throw new IllegalArgumentException("토큰과 사용자 정보가 일치하지 않습니다.");
        }
        
        // 새 토큰 생성
        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getId());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId());
        LocalDateTime newRefreshTokenExpiresAt = jwtTokenProvider.getRefreshTokenExpiration();
        
        // Refresh Token 업데이트
        token.updateRefreshToken(newRefreshToken, newRefreshTokenExpiresAt);
        tokenRepository.save(token);
        
        log.info("토큰 갱신 완료 - userId: {}", user.getId());
        
        return new TokenResponse(newAccessToken, newRefreshToken);
    }
}
