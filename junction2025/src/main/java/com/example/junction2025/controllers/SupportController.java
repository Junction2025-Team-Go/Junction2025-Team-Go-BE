package com.example.junction2025.controllers;

import com.example.junction2025.dto.response.SupportCountResponse;
import com.example.junction2025.services.SupportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Support", description = "서포터 카운트 API")
public class SupportController {
    
    private final SupportService supportService;

    @PostMapping("/support")
    @Operation(summary = "서포터 카운트 증가", 
               description = "서포터 카운트를 1 증가시키고 총 카운트를 반환합니다.")
    public ResponseEntity<SupportCountResponse> support() {
        try {
            int count = supportService.increaseSupportCount();
            SupportCountResponse response = new SupportCountResponse(count);
            log.info("서포터 카운트 증가 완료: {}", count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("서포터 카운트 증가 중 오류 발생", e);
            // 오류 발생 시에도 현재 카운트 반환
            int count = supportService.getSupportCount();
            return ResponseEntity.ok(new SupportCountResponse(count));
        }
    }

    @GetMapping("/supporters")
    @Operation(summary = "서포터 카운트 조회", 
               description = "현재 총 서포터 카운트를 반환합니다.")
    public ResponseEntity<SupportCountResponse> getSupportCount() {
        try {
            int count = supportService.getSupportCount();
            SupportCountResponse response = new SupportCountResponse(count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("서포터 카운트 조회 중 오류 발생", e);
            return ResponseEntity.ok(new SupportCountResponse(0));
        }
    }
}
