package com.example.junction2025.services;

import com.example.junction2025.domain.Review;
import com.example.junction2025.domain.ShopType;
import com.example.junction2025.domain.Store;
import com.example.junction2025.dto.response.GetShopInfoResponse;
import com.example.junction2025.repository.ReviewRepository;
import com.example.junction2025.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoreService {
    private static final double SEARCH_RADIUS_KM = 20.0; // 검색 반경 20km
    private static final double LAT_DEGREE_KM = 111.0; // 위도 1도 ≈ 111km
    private static final int MAX_RESULTS = 50; // 최대 반환 개수
    
    private final StoreRepository storeRepository;
    private final ReviewRepository reviewRepository;
    
    @Transactional(readOnly = true)
    public List<GetShopInfoResponse> getShopInfoResponses(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return new ArrayList<>();
        }
        
        // 반경 내 Store 검색을 위한 범위 계산
        double latRange = SEARCH_RADIUS_KM / LAT_DEGREE_KM;
        double lngRange = SEARCH_RADIUS_KM / (LAT_DEGREE_KM * Math.cos(Math.toRadians(latitude)));
        
        double minLat = latitude - latRange;
        double maxLat = latitude + latRange;
        double minLng = longitude - lngRange;
        double maxLng = longitude + lngRange;
        
        // 범위 내 Store 조회
        List<Store> stores = storeRepository.findByLocationRange(minLat, maxLat, minLng, maxLng);
        
        // 거리 계산 결과를 캐싱하여 중복 계산 방지 (필터링과 정렬에서 각각 계산하는 것을 방지)
        Map<Store, Double> distanceMap = new HashMap<>();
        for (Store store : stores) {
            double distance = calculateDistance(latitude, longitude, 
                    store.getLatitude(), store.getLongitude());
            distanceMap.put(store, distance);
        }
        
        // 거리순으로 정렬하고 반경 내 Store만 필터링
        List<Store> nearbyStores = stores.stream()
                .filter(store -> distanceMap.get(store) <= SEARCH_RADIUS_KM)
                .sorted(Comparator.comparingDouble(distanceMap::get))
                .limit(MAX_RESULTS)
                .collect(Collectors.toList());
        
        // Store ID 목록 추출하여 리뷰를 한 번에 조회 (N+1 문제 방지)
        List<Long> storeIds = nearbyStores.stream()
                .map(Store::getId)
                .collect(Collectors.toList());
        
        // 모든 리뷰를 한 번에 조회하여 N+1 문제 방지
        final Map<Long, List<Review>> reviewsMap;
        if (!storeIds.isEmpty()) {
            List<Review> allReviews = reviewRepository.findByStoreIds(storeIds);
            // Store ID별로 그룹화
            reviewsMap = allReviews.stream()
                    .collect(Collectors.groupingBy(review -> review.getStore().getId(), 
                            Collectors.toList()));
        } else {
            reviewsMap = new HashMap<>();
        }
        
        // Store를 GetShopInfoResponse로 변환 (리뷰 맵 전달)
        final Map<Long, List<Review>> finalReviewsMap = reviewsMap;
        return nearbyStores.stream()
                .map(store -> convertToResponse(store, finalReviewsMap.getOrDefault(store.getId(), new ArrayList<>())))
                .collect(Collectors.toList());
    }
    
    /**
     * Store 엔티티를 GetShopInfoResponse로 변환합니다.
     */
    private GetShopInfoResponse convertToResponse(Store store, List<Review> reviews) {
        // ShopType 결정 (types에서 "restaurant" 또는 "cafe" 확인)
        ShopType shopType = determineShopType(store.getTypes());
        
        // 영업 시간 포맷팅 (오늘 요일 기준 또는 첫 번째 영업일)
        String openTime = formatOpeningHours(store.getOpenTime(), store.getCloseTime());
        
        // 리뷰 텍스트 추출 (최대 5개)
        List<String> comments = reviews.stream()
                .map(Review::getText)
                .limit(5)
                .collect(Collectors.toList());
        
        return new GetShopInfoResponse(
                store.getId().toString(), // shopId
                store.getLatitude(),
                store.getLongitude(),
                null, // videoUrl (현재 Store에 없음)
                store.getPhotoUrl() != null ? store.getPhotoUrl() : "", // shopImageUrl
                store.getRating() != null ? store.getRating() : 0.0, // rating
                store.getUserRatingsTotal() != null ? store.getUserRatingsTotal() : 0, // rating_count
                shopType,
                store.getAddress() != null ? store.getAddress() : "", // locationString
                openTime,
                comments
        );
    }
    
    /**
     * Store의 types에서 ShopType을 결정합니다.
     */
    private ShopType determineShopType(List<String> types) {
        if (types == null || types.isEmpty()) {
            return ShopType.Cafe; // 기본값
        }
        
        // "restaurant"가 포함되어 있으면 Restaurant
        if (types.stream().anyMatch(type -> type != null && type.toLowerCase().contains("restaurant"))) {
            return ShopType.Restaurant;
        }
        
        // "cafe"가 포함되어 있으면 Cafe
        if (types.stream().anyMatch(type -> type != null && type.toLowerCase().contains("cafe"))) {
            return ShopType.Cafe;
        }
        
        // 기본값은 Cafe
        return ShopType.Cafe;
    }
    
    /**
     * 영업 시간을 포맷팅합니다.
     * 오늘 요일의 영업 시간을 반환하거나, 첫 번째 영업일의 시간을 반환합니다.
     */
    private String formatOpeningHours(List<String> openTime, List<String> closeTime) {
        if (openTime == null || closeTime == null || 
            openTime.size() != 7 || closeTime.size() != 7) {
            return null;
        }
        
        // 오늘 요일 인덱스 (월요일=0, 일요일=6)
        // Java DayOfWeek: 월요일=1, 일요일=7
        int dayOfWeek = java.time.LocalDate.now().getDayOfWeek().getValue();
        int todayIndex = (dayOfWeek == 7) ? 6 : (dayOfWeek - 1); // 일요일은 6, 월요일은 0
        
        // 오늘 영업 시간이 있으면 반환
        if (todayIndex < openTime.size() && todayIndex < closeTime.size() &&
            openTime.get(todayIndex) != null && closeTime.get(todayIndex) != null) {
            return openTime.get(todayIndex) + " - " + closeTime.get(todayIndex);
        }
        
        // 오늘 영업하지 않으면 첫 번째 영업일 찾기
        for (int i = 0; i < 7; i++) {
            if (openTime.get(i) != null && closeTime.get(i) != null) {
                return openTime.get(i) + " - " + closeTime.get(i);
            }
        }
        
        return "영업 시간 정보 없음";
    }
    
    /**
     * 두 지점 간의 거리를 계산합니다 (Haversine 공식).
     */
    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        final int R = 6371; // 지구 반경 (km)
        
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLng / 2) * Math.sin(dLng / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }
}
