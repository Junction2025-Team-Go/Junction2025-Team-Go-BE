package com.example.junction2025.controllers;

import com.example.junction2025.dto.ApiResponse;
import com.example.junction2025.dto.request.RefreshTokenRequest;
import com.example.junction2025.dto.request.RegisterUserRequest;
import com.example.junction2025.dto.response.TokenResponse;
import com.example.junction2025.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "사용자 등록/로그인", 
               description = "구글 또는 애플 idToken으로 사용자를 등록하거나 로그인합니다.")
    public ResponseEntity<ApiResponse<TokenResponse>> registerUser(@RequestBody RegisterUserRequest request) {
        try {
            TokenResponse response = authService.registerUser(request.getIdToken());
            return ResponseEntity.ok(ApiResponse.success("로그인 성공", response));
        } catch (IllegalArgumentException e) {
            log.error("사용자 등록 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            log.error("사용자 등록 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "서버 오류가 발생했습니다."));
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "토큰 갱신", 
               description = "Refresh Token으로 새로운 Access Token과 Refresh Token을 발급합니다.")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(@RequestBody RefreshTokenRequest request) {
        try {
            TokenResponse response = authService.refreshUser(request.getRefreshToken());
            return ResponseEntity.ok(ApiResponse.success("토큰 갱신 성공", response));
        } catch (IllegalArgumentException e) {
            log.error("토큰 갱신 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            log.error("토큰 갱신 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "서버 오류가 발생했습니다."));
        }
    }
}
