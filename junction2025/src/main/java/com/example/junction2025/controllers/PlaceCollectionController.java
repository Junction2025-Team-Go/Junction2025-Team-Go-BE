//package com.example.junction2025.controllers;
//
//import com.example.junction2025.dto.ApiResponse;
//import com.example.junction2025.services.PlaceCollectionService;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.Parameter;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//@Slf4j
//@RestController
//@RequestMapping("/api/places")
//@RequiredArgsConstructor
//@Tag(name = "Place Collection", description = "상점 데이터 수집 API (음식점 및 일반 상점 포함)")
//public class PlaceCollectionController {
//
//    private final PlaceCollectionService placeCollectionService;
//
//    @PostMapping("/collect")
//    @Operation(summary = "상점 데이터 수집 (클라이언트에서 호출 필요 X)",
//               description = "지정된 위치 기준 반경 20km 내의 모든 상점 데이터를 수집하여 DB에 저장합니다 ")
//    public ResponseEntity<ApiResponse<PlaceCollectionService.CollectionStatistics>> collectStores(
//            @Parameter(description = "중심 위도", example = "60.1733244")
//            @RequestParam double latitude,
//            @Parameter(description = "중심 경도", example = "24.9410248")
//            @RequestParam double longitude
//    ) {
//        log.info("상점 수집 요청 - 위치: ({}, {})", latitude, longitude);
//
//        try {
//            PlaceCollectionService.CollectionStatistics statistics =
//                placeCollectionService.collectAllStores(latitude, longitude);
//
//            return ResponseEntity.ok(ApiResponse.success(
//                "상점 수집이 완료되었습니다.",
//                statistics
//            ));
//
//        } catch (Exception e) {
//            log.error("상점 수집 중 오류 발생", e);
//            return ResponseEntity.internalServerError()
//                .body(ApiResponse.error(500, "상점 수집 중 오류가 발생했습니다: " + e.getMessage()));
//        }
//    }
//}
//
