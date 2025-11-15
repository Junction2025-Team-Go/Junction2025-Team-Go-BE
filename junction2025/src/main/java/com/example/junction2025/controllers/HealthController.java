package com.example.junction2025.controllers;

import com.example.junction2025.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/health")
public class HealthController {
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Void>> getHealth() {
        return ResponseEntity.ok(ApiResponse.success("health ok", null));
    }
}
