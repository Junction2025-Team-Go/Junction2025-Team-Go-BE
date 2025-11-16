//package com.example.junction2025.utils;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//import java.util.List;
//
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//
//import com.example.junction2025.utils.ReviewParseManager.RestaurantData;
//
///**
// * ReviewParseManager 테스트 클래스
// */
//@SpringBootTest
//class ReviewParseManagerTest {
//
//    @Autowired
//    ReviewParseManager reviewParseManager;
//
//    @Test
//    @DisplayName("ReviewParseManager 빈 주입 테스트")
//    void testReviewParseManagerInjection() {
//        assertThat(reviewParseManager).isNotNull();
//    }
//
//    @Test
//    @DisplayName("서울 시청 위치 기준 음식점 수집 테스트")
//    void testCollectRestaurantsSeoulCityHall() {
//        // 서울 시청 좌표 (위도: 37.5665, 경도: 126.9780)
//        // 헬싱키 좌표 (위도: 60.1733244, 경도: 24.9410248)
//        double latitude = 60.1733244;
//        double longitude = 24.9410248;
//
//        List<RestaurantData> restaurants = reviewParseManager.collectRestaurants(latitude, longitude);
//
//        // 결과가 null이 아니어야 함
//        assertThat(restaurants).isNotNull();
//
//        // 리뷰가 수집된 경우 출력
//        if (!restaurants.isEmpty()) {
//            System.out.println("수집된 음식점 개수: " + restaurants.size());
//
//            // 첫 번째 음식점 정보 출력
//            RestaurantData firstRestaurant = restaurants.get(0);
//            System.out.println("음식점 이름: " + firstRestaurant.name());
//            System.out.println("주소: " + firstRestaurant.address());
//            System.out.println("위치: (" + firstRestaurant.latitude() + ", " + firstRestaurant.longitude() + ")");
//            System.out.println("평점: " + firstRestaurant.rating());
//            System.out.println("리뷰 개수: " + firstRestaurant.reviews().size());
//
//            // 필수 필드 검증
//            assertThat(firstRestaurant.placeId()).isNotNull();
//            assertThat(firstRestaurant.name()).isNotNull();
//            assertThat(firstRestaurant.latitude()).isNotNull();
//            assertThat(firstRestaurant.longitude()).isNotNull();
//        } else {
//            System.out.println("수집된 음식점이 없습니다.");
//        }
//    }
//
//    @Test
//    @DisplayName("강남역 위치 기준 음식점 수집 테스트")
//    void testCollectRestaurantsGangnamStation() {
//        // 강남역 좌표 (위도: 37.4980, 경도: 127.0276)
//        double latitude = 37.4980;
//        double longitude = 127.0276;
//
//        List<RestaurantData> restaurants = reviewParseManager.collectRestaurants(latitude, longitude);
//
//        assertThat(restaurants).isNotNull();
//
//        System.out.println("강남역 기준 수집된 음식점 개수: " + restaurants.size());
//
//        // 음식점이 있는 경우 데이터 구조 검증
//        if (!restaurants.isEmpty()) {
//            RestaurantData restaurant = restaurants.get(0);
//            assertThat(restaurant.placeId()).isNotEmpty();
//            assertThat(restaurant.name()).isNotEmpty();
//            assertThat(restaurant.reviews()).isNotNull();
//        }
//    }
//
//    @Test
//    @DisplayName("RestaurantData 구조 검증 테스트")
//    void testRestaurantDataStructure() {
//        // 서울 시청 좌표로 테스트
//        double latitude = 37.5665;
//        double longitude = 126.9780;
//
//        List<RestaurantData> restaurants = reviewParseManager.collectRestaurants(latitude, longitude);
//
//        assertThat(restaurants).isNotNull();
//
//        // 음식점이 있는 경우 구조 검증
//        if (!restaurants.isEmpty()) {
//            RestaurantData restaurant = restaurants.get(0);
//
//            // 필수 필드 검증
//            assertThat(restaurant.placeId()).isNotNull().isNotEmpty();
//            assertThat(restaurant.name()).isNotNull().isNotEmpty();
//            assertThat(restaurant.latitude()).isNotNull();
//            assertThat(restaurant.longitude()).isNotNull();
//            assertThat(restaurant.types()).isNotNull();
//            assertThat(restaurant.reviews()).isNotNull();
//
//            System.out.println("RestaurantData 구조 검증 완료:");
//            System.out.println("  - Place ID: " + restaurant.placeId());
//            System.out.println("  - 이름: " + restaurant.name());
//            System.out.println("  - 주소: " + restaurant.address());
//            System.out.println("  - 위치: (" + restaurant.latitude() + ", " + restaurant.longitude() + ")");
//            System.out.println("  - 평점: " + restaurant.rating());
//            System.out.println("  - 리뷰 개수: " + restaurant.reviews().size());
//            System.out.println("  - 타입: " + restaurant.types());
//        }
//    }
//}
//
