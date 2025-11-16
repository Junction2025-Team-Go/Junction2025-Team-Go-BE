//package com.example.junction2025.controllers;
//
//import com.example.junction2025.dto.ApiResponse;
//import com.example.junction2025.dto.request.GenerateVideoRequest;
//import com.example.junction2025.dto.response.GenerateVideoResponse;
//import com.example.junction2025.services.Veo3Service;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//@Slf4j
//@RestController
//@RequestMapping("/api/veo3")
//@RequiredArgsConstructor
//@Tag(name = "Veo3", description = "Veo3 비디오 생성 API")
//public class Veo3Controller {
//
//    private final Veo3Service veo3Service;
//
//    @PostMapping("/generate-video")
//    @Operation(summary = "비디오 생성",
//               description = "이미지 3개와 텍스트 프롬프트를 받아 Veo3 API로 비디오를 생성하고 S3에 업로드합니다.")
//    public ResponseEntity<ApiResponse<GenerateVideoResponse>> generateVideo(
//            @RequestBody GenerateVideoRequest request) {
//        try {
//            log.info("비디오 생성 요청 수신 - 프롬프트: {}", request.getPrompt());
//
//            GenerateVideoResponse response = veo3Service.generateVideo(request);
//
//            return ResponseEntity.ok(
//                ApiResponse.success("비디오 생성 완료", response)
//            );
//
//        } catch (IllegalArgumentException e) {
//            log.error("비디오 생성 요청 검증 실패", e);
//            return ResponseEntity.badRequest()
//                    .body(ApiResponse.error(400, e.getMessage()));
//        } catch (Exception e) {
//            log.error("비디오 생성 중 오류 발생", e);
//            return ResponseEntity.internalServerError()
//                    .body(ApiResponse.error(500, "비디오 생성 중 오류가 발생했습니다: " + e.getMessage()));
//        }
//    }
//}
//
