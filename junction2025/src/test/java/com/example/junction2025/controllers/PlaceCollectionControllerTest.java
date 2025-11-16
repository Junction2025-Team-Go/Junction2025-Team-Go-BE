package com.example.junction2025.controllers;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.example.junction2025.dto.ApiResponse;
import com.example.junction2025.services.PlaceCollectionService;

/**
 * PlaceCollectionController 테스트 클래스
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PlaceCollectionControllerTest {
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private PlaceCollectionController placeCollectionController;
    
    @Test
    @DisplayName("PlaceCollectionController 빈 주입 테스트")
    void testControllerInjection() {
        assertThat(placeCollectionController).isNotNull();
    }
    
    @Test
    @DisplayName("음식점 수집 API 엔드포인트 테스트")
    @Transactional
    void testCollectRestaurantsEndpoint() {
        String url = "http://localhost:" + port + "/api/places/collect?latitude=37.5665&longitude=126.9780";
        
        ResponseEntity<ApiResponse> response = restTemplate.postForEntity(
            url, null, ApiResponse.class
        );
        
        // 응답 상태 확인
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(200);
        
        System.out.println("API 응답:");
        System.out.println("  - 상태 코드: " + response.getBody().getStatus());
        System.out.println("  - 메시지: " + response.getBody().getMessage());
    }
    
    @Test
    @DisplayName("잘못된 파라미터로 API 호출 테스트")
    void testCollectRestaurantsWithInvalidParameters() {
        // 위도/경도가 범위를 벗어나는 경우
        String url = "http://localhost:" + port + "/api/places/collect?latitude=999&longitude=999";
        
        ResponseEntity<ApiResponse> response = restTemplate.postForEntity(
            url, null, ApiResponse.class
        );
        
        // 에러 응답이거나 빈 결과가 반환될 수 있음
        assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

