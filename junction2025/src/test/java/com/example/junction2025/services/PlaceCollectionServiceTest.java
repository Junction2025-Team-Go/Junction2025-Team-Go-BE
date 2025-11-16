package com.example.junction2025.services;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.example.junction2025.repository.RestaurantRepository;
import com.example.junction2025.repository.ReviewRepository;

/**
 * PlaceCollectionService 테스트 클래스
 */
@SpringBootTest
@ActiveProfiles("test")
class PlaceCollectionServiceTest {
    
    @Autowired
    PlaceCollectionService placeCollectionService;
    
    @Autowired
    RestaurantRepository restaurantRepository;
    
    @Autowired
    ReviewRepository reviewRepository;
    
    @Test
    @DisplayName("PlaceCollectionService 빈 주입 테스트")
    void testPlaceCollectionServiceInjection() {
        assertThat(placeCollectionService).isNotNull();
    }
    
    @Test
    @DisplayName("서울 시청 위치 기준 전체 음식점 수집 테스트")
    @Transactional
    void testCollectAllRestaurantsSeoulCityHall() {
        // 서울 시청 좌표 (위도: 37.5665, 경도: 126.9780)
        double latitude = 37.5665;
        double longitude = 126.9780;
        
        // 수집 전 DB 상태
        long restaurantCountBefore = restaurantRepository.count();
        long reviewCountBefore = reviewRepository.count();
        
        System.out.println("수집 전 음식점 개수: " + restaurantCountBefore);
        System.out.println("수집 전 리뷰 개수: " + reviewCountBefore);
        
        // 음식점 수집 실행
        PlaceCollectionService.CollectionStatistics statistics = 
            placeCollectionService.collectAllRestaurants(latitude, longitude);
        
        // 결과 검증
        assertThat(statistics).isNotNull();
        assertThat(statistics.gridPointsProcessed()).isGreaterThan(0);
        assertThat(statistics.uniqueRestaurantsFound()).isGreaterThanOrEqualTo(0);
        assertThat(statistics.restaurantsSaved()).isGreaterThanOrEqualTo(0);
        assertThat(statistics.reviewsSaved()).isGreaterThanOrEqualTo(0);
        
        System.out.println("수집 통계:");
        System.out.println("  - 처리된 격자 포인트: " + statistics.gridPointsProcessed());
        System.out.println("  - 발견된 고유 음식점: " + statistics.uniqueRestaurantsFound());
        System.out.println("  - 저장된 음식점: " + statistics.restaurantsSaved());
        System.out.println("  - 업데이트된 음식점: " + statistics.restaurantsUpdated());
        System.out.println("  - 저장된 리뷰: " + statistics.reviewsSaved());
        
        // 수집 후 DB 상태
        long restaurantCountAfter = restaurantRepository.count();
        long reviewCountAfter = reviewRepository.count();
        
        System.out.println("수집 후 음식점 개수: " + restaurantCountAfter);
        System.out.println("수집 후 리뷰 개수: " + reviewCountAfter);
        
        // DB에 저장되었는지 확인
        if (statistics.restaurantsSaved() > 0) {
            assertThat(restaurantCountAfter).isGreaterThanOrEqualTo(restaurantCountBefore);
        }
    }
    
    @Test
    @DisplayName("강남역 위치 기준 전체 음식점 수집 테스트")
    @Transactional
    void testCollectAllRestaurantsGangnamStation() {
        // 강남역 좌표 (위도: 37.4980, 경도: 127.0276)
        double latitude = 37.4980;
        double longitude = 127.0276;
        
        PlaceCollectionService.CollectionStatistics statistics = 
            placeCollectionService.collectAllRestaurants(latitude, longitude);
        
        assertThat(statistics).isNotNull();
        System.out.println("강남역 기준 수집 통계: " + statistics);
    }
    
    @Test
    @DisplayName("중복 수집 테스트 - 같은 위치에서 두 번 수집")
    @Transactional
    void testDuplicateCollection() {
        // 서울 시청 좌표
        double latitude = 37.5665;
        double longitude = 126.9780;
        
        // 첫 번째 수집
        PlaceCollectionService.CollectionStatistics firstStats = 
            placeCollectionService.collectAllRestaurants(latitude, longitude);
        
        long firstRestaurantCount = restaurantRepository.count();
        long firstReviewCount = reviewRepository.count();
        
        System.out.println("첫 번째 수집:");
        System.out.println("  - 음식점: " + firstStats.uniqueRestaurantsFound());
        System.out.println("  - 저장된 음식점: " + firstStats.restaurantsSaved());
        System.out.println("  - 저장된 리뷰: " + firstStats.reviewsSaved());
        
        // 두 번째 수집 (중복 제거 확인)
        PlaceCollectionService.CollectionStatistics secondStats = 
            placeCollectionService.collectAllRestaurants(latitude, longitude);
        
        long secondRestaurantCount = restaurantRepository.count();
        long secondReviewCount = reviewRepository.count();
        
        System.out.println("두 번째 수집:");
        System.out.println("  - 음식점: " + secondStats.uniqueRestaurantsFound());
        System.out.println("  - 저장된 음식점: " + secondStats.restaurantsSaved());
        System.out.println("  - 업데이트된 음식점: " + secondStats.restaurantsUpdated());
        System.out.println("  - 저장된 리뷰: " + secondStats.reviewsSaved());
        
        // 두 번째 수집에서는 업데이트가 발생하거나 새로운 데이터가 적을 수 있음
        assertThat(secondStats.uniqueRestaurantsFound()).isGreaterThanOrEqualTo(0);
        
        // 음식점 개수는 같거나 증가해야 함 (중복 제거로 인해 같을 수 있음)
        assertThat(secondRestaurantCount).isGreaterThanOrEqualTo(firstRestaurantCount);
    }
}

