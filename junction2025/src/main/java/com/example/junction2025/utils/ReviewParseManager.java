package com.example.junction2025.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.example.junction2025.domain.Store;
import com.example.junction2025.domain.Review;
import com.example.junction2025.repository.StoreRepository;
import com.example.junction2025.repository.ReviewRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * 구글 맵에서 위치 기반 맛집 리뷰를 크롤링하는 매니저.
 * <p>
 * Google Places API를 사용하여 위치 기반 맛집 정보와 리뷰를 수집합니다.
 */
@Slf4j
@Component
public class ReviewParseManager {
    
    private static final int RADIUS_METERS = 20000; // 20km
    private static final int REQUEST_DELAY_MS = 500; // 요청 간 지연 시간 (API 제한 고려하여 최소화)
    private static final int MIN_REVIEW_COUNT = 5; // 최소 리뷰 개수
    private static final int MIN_PHOTO_COUNT = 10; // 최소 사진 개수
    
    // 음식점 및 상점 관련 타입 필터링을 위한 타입 목록
    private static final List<String> STORE_TYPES = List.of(
        // 음식점 타입
        "restaurant",
        "cafe"
//        "food",
//        "cafe",
//        "meal_takeaway",
//        "meal_delivery",
//        "bakery",
//        "bar",
//        "meal_drive_through",
//        // 상점 타입
//        "store",
//        "shopping_mall",
//        "supermarket",
//        "convenience_store",
//        "department_store",
//        "clothing_store",
//        "shoe_store",
//        "jewelry_store",
//        "book_store",
//        "electronics_store",
//        "furniture_store",
//        "home_goods_store",
//        "grocery_or_supermarket",
//        "market"
    );
    
    private final RestClient restClient;
    private final String googlePlacesApiKey;
    private final com.example.junction2025.services.Veo3Service veo3Service;
    private final StoreRepository storeRepository;
    private final ReviewRepository reviewRepository;
    
    public ReviewParseManager(
            RestClient.Builder restClientBuilder,
            @Value("${google.places.api.key:}") String googlePlacesApiKey,
            com.example.junction2025.services.Veo3Service veo3Service,
            StoreRepository storeRepository,
            ReviewRepository reviewRepository
    ) {
        this.restClient = restClientBuilder.build();
        this.googlePlacesApiKey = googlePlacesApiKey;
        this.veo3Service = veo3Service;
        this.storeRepository = storeRepository;
        this.reviewRepository = reviewRepository;
    }
    
    /**
     * 주어진 위치 기준 반경 20km 내의 상점 정보를 수집합니다 (restaurant와 cafe만).
     * Google Places Text Search API를 사용하여 "restaurant"와 "cafe"로 각각 검색합니다.
     * 리뷰 5개 이상, 사진 10개 이상인 상점만 반환합니다.
     * 각 상점의 비디오 생성 및 S3 업로드가 완료되면 즉시 DB에 저장합니다.
     *
     * @param latitude  위도
     * @param longitude 경도
     * @return 저장된 상점 개수 통계
     */
    @Transactional
    public StoreCollectionResult collectStores(double latitude, double longitude) {
        List<Store> stores = new ArrayList<>();
        Set<String> processedPlaceIds = new HashSet<>(); // 중복 제거를 위한 Set
        
        // 통계 변수
        int savedCount = 0;
        int updatedCount = 0;
        int reviewCount = 0;
        
        try {
            // "restaurant"와 "cafe"로 각각 검색
            String[] searchQueries = {"restaurant", "cafe"};
            
            for (String query : searchQueries) {
                try {
                    // Text Search API를 사용하여 위치 기반 검색
                    String textSearchUrl = String.format(
                        "https://maps.googleapis.com/maps/api/place/textsearch/json?query=%s&location=%s,%s&radius=%d&key=%s",
                        query, latitude, longitude, RADIUS_METERS, googlePlacesApiKey
                    );
                    
                    log.info("Google Places Text Search API 호출: 쿼리='{}', 위치 ({}, {})", query, latitude, longitude);
                    log.debug("API URL: {}", textSearchUrl);
                    
                    // 먼저 응답을 String으로 받아서 확인
                    String responseBody = null;
                    PlacesApiResponse textSearchResponse = null;
                    
                    try {
                        responseBody = restClient.get()
                                .uri(textSearchUrl)
                                .retrieve()
                                .body(String.class);
                        
                        log.debug("API 응답 (첫 500자): {}", 
                            responseBody != null && responseBody.length() > 500 ? 
                                responseBody.substring(0, 500) + "..." : responseBody);
                        
                        // JSON 파싱 (null 필드 무시)
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
                        textSearchResponse = mapper.readValue(responseBody, PlacesApiResponse.class);
                        
                    } catch (Exception e) {
                        log.error("API 응답 파싱 실패 (쿼리: {}): {}", query, e.getMessage(), e);
                        if (responseBody != null) {
                            log.error("응답 본문 (첫 1000자): {}", 
                                responseBody.length() > 1000 ? responseBody.substring(0, 1000) + "..." : responseBody);
                        }
                        continue;
                    }
                    
                    if (textSearchResponse == null) {
                        log.warn("Text Search API 응답이 null입니다. 쿼리: {}", query);
                        continue;
                    }
                    
                    // API 응답 상태 확인
                    String status = textSearchResponse.status();
                    if (!"OK".equals(status) && !"ZERO_RESULTS".equals(status)) {
                        String errorMsg = textSearchResponse.error_message();
                        log.error("Text Search API 오류 - 상태: {}, 메시지: {}, 쿼리: {}", 
                            status, errorMsg != null ? errorMsg : "없음", query);
                        if (responseBody != null) {
                            log.error("응답 본문: {}", responseBody);
                        }
                        continue;
                    }
                    
                    if ("ZERO_RESULTS".equals(status) || textSearchResponse.results() == null || textSearchResponse.results().isEmpty()) {
                        log.debug("검색 결과가 없습니다. (status: {}, 쿼리: {})", status, query);
                        continue;
                    }
                    
                    log.debug("쿼리 '{}'로 위치 ({}, {})에서 {}개의 결과 발견", 
                        query, latitude, longitude, textSearchResponse.results().size());
                    
                    // 각 결과 처리
                    for (PlaceResult place : textSearchResponse.results()) {
                        // 중복 제거 (place_id 기준)
                        if (processedPlaceIds.contains(place.place_id())) {
                            continue;
                        }
                        processedPlaceIds.add(place.place_id());
                        
                        try {
                            log.debug("상점 상세 정보 조회 시작: {} (place_id: {})", place.name(), place.place_id());
                            Store store = getStoreDetails(place.place_id());
                            if (store != null) {
                                stores.add(store);
                                log.debug("조건을 만족하는 상점 추가: {} (리뷰: {}개, 사진: {}개)", 
                                    store.getName(), 
                                    store.getReviews() != null ? store.getReviews().size() : 0,
                                    store.getPhotoUrl() != null ? "있음" : "없음");
                            } else {
                                log.debug("상점 '{}'이(가) null로 반환됨 (최소 요구사항 미충족 또는 API 오류)", place.name());
                            }
                            
                            // API 호출 제한을 위한 지연 (최소화)
                            TimeUnit.MILLISECONDS.sleep(REQUEST_DELAY_MS);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            log.warn("상점 정보 수집이 중단되었습니다.", e);
                            break;
                        } catch (Exception e) {
                            log.error("상점 상세 정보 조회 실패: {} - {}", place.name(), e.getMessage(), e);
                        }
                    }
                    
                    // 쿼리 간 지연 (최소화)
                    TimeUnit.MILLISECONDS.sleep(REQUEST_DELAY_MS);
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("상점 정보 수집이 중단되었습니다.", e);
                    break;
                } catch (Exception e) {
                    log.error("Text Search API 호출 실패 (쿼리: {}): {}", query, e.getMessage(), e);
                }
            }
            
            log.info("위치 ({}, {})에서 총 {}개의 상점이 수집되었습니다.", latitude, longitude, stores.size());
            
            // 모든 상점 정보와 리뷰 수집 완료 후, 각 상점에 대해 비디오 생성 및 S3 업로드
            // 각 상점의 비디오 생성이 완료되면 즉시 DB에 저장
            log.info("상점별 비디오 생성 및 S3 업로드 시작 (총 {}개 상점)", stores.size());
            int videoGeneratedCount = 0;
            int videoFailedCount = 0;
            
            for (Store store : stores) {
                if (store == null) {
                    log.warn("상점이 null입니다. 건너뜀");
                    continue;
                }
                
                // stores 리스트에 이미 추가된 상점들은 모두 처리해야 하므로 중복 체크 불필요
                
                if (store.getReviews() == null || store.getReviews().isEmpty()) {
                    log.info("상점 '{}'은(는) 리뷰가 없어 비디오 생성 건너뜀", store.getName());
                    // 리뷰가 없어도 DB에 저장 (videoUrl은 null)
                    try {
                        boolean alreadyExists = storeRepository.existsByPlaceId(store.getPlaceId());
                        Store savedStore = saveStoreToDatabase(store);
                        if (alreadyExists) {
                            updatedCount++;
                        } else {
                            savedCount++;
                        }
                        log.info("상점 '{}' 저장 완료 (리뷰 없음)", store.getName());
                    } catch (Exception e) {
                        log.error("상점 '{}' DB 저장 중 오류 발생", store.getName(), e);
                    }
                    continue;
                }
                
                try {
                    log.info("상점 '{}' 비디오 생성 및 S3 업로드 시작 (리뷰 {}개)", 
                        store.getName(), store.getReviews().size());
                    
                    String videoUrl = veo3Service.generateVideoForStore(store, store.getReviews());
                    
                    if (videoUrl != null && !videoUrl.isEmpty()) {
                        store.setVideoUrl(videoUrl);
                        videoGeneratedCount++;
                        log.info("상점 '{}' 비디오 생성 및 S3 업로드 완료 - videoUrl 저장됨: {}", 
                            store.getName(), videoUrl);
                    } else {
                        videoFailedCount++;
                        log.warn("상점 '{}' 비디오 생성 실패 또는 S3 업로드 실패 (조건 미충족 또는 오류)", 
                            store.getName());
                        store.setVideoUrl(null);
                    }
                } catch (Exception e) {
                    videoFailedCount++;
                    log.error("상점 '{}' 비디오 생성 중 오류 발생 (계속 진행)", store.getName(), e);
                    store.setVideoUrl(null);
                }
                
                // 비디오 생성 완료 후 즉시 DB에 저장
                try {
                    log.info("상점 '{}' DB 저장 시작 (videoUrl: {})", 
                        store.getName(), store.getVideoUrl() != null ? "있음" : "없음");
                    
                    boolean alreadyExists = storeRepository.existsByPlaceId(store.getPlaceId());
                    Store savedStore = saveStoreToDatabase(store);
                    int savedReviews = saveReviewsToDatabase(savedStore, store.getReviews());
                    reviewCount += savedReviews;
                    
                    if (alreadyExists) {
                        updatedCount++;
                        log.info("상점 '{}' 업데이트 완료 (videoUrl: {}, 리뷰: {}개)", 
                            store.getName(), 
                            store.getVideoUrl() != null ? "있음" : "없음",
                            savedReviews);
                    } else {
                        savedCount++;
                        log.info("상점 '{}' 저장 완료 (videoUrl: {}, 리뷰: {}개)", 
                            store.getName(), 
                            store.getVideoUrl() != null ? "있음" : "없음",
                            savedReviews);
                    }
                } catch (Exception e) {
                    log.error("상점 '{}' DB 저장 중 오류 발생", store.getName(), e);
                    e.printStackTrace();
                }
                
                // API 호출 제한을 위한 지연 (비디오 생성은 시간이 걸리므로 짧은 지연)
                try {
                    TimeUnit.MILLISECONDS.sleep(REQUEST_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("비디오 생성이 중단되었습니다.", e);
                    break;
                }
            }
            
            log.info("비디오 생성 완료 - 성공: {}개, 실패: {}개 (총 {}개 상점)", 
                videoGeneratedCount, videoFailedCount, stores.size());
            log.info("DB 저장 완료 - 신규: {}개, 업데이트: {}개, 리뷰: {}개", 
                savedCount, updatedCount, reviewCount);
            
        } catch (RestClientResponseException e) {
            log.error("Google Places API 호출 실패 - HTTP {}: {}", 
                e.getStatusCode().value(), e.getResponseBodyAsString(), e);
        } catch (RestClientException e) {
            log.error("Google Places API 통신 오류", e);
        }
        
        return new StoreCollectionResult(stores.size(), savedCount, updatedCount, reviewCount);
    }
    
    /**
     * Store를 DB에 저장하거나 업데이트합니다.
     */
    private Store saveStoreToDatabase(Store store) {
        java.util.Optional<Store> existing = storeRepository.findByPlaceId(store.getPlaceId());
        
        Store savedStore;
        if (existing.isPresent()) {
            // 이미 존재하는 상점은 업데이트
            savedStore = existing.get();
            savedStore.updateInfo(
                store.getName(),
                store.getAddress(),
                store.getRating(),
                store.getUserRatingsTotal(),
                store.getPhotoUrl(),
                store.getTypes()
            );
            // 영업 시간도 업데이트
            if (store.getOpenTime() != null && store.getCloseTime() != null) {
                savedStore.getOpenTime().clear();
                savedStore.getOpenTime().addAll(store.getOpenTime());
                savedStore.getCloseTime().clear();
                savedStore.getCloseTime().addAll(store.getCloseTime());
            }
            // videoUrl도 업데이트
            if (store.getVideoUrl() != null && !store.getVideoUrl().isEmpty()) {
                savedStore.setVideoUrl(store.getVideoUrl());
            }
        } else {
            // 새로운 상점은 그대로 저장
            List<String> openTime = store.getOpenTime() != null ? store.getOpenTime() : new ArrayList<>();
            List<String> closeTime = store.getCloseTime() != null ? store.getCloseTime() : new ArrayList<>();
            
            savedStore = Store.builder()
                .placeId(store.getPlaceId())
                .name(store.getName())
                .address(store.getAddress())
                .latitude(store.getLatitude())
                .longitude(store.getLongitude())
                .rating(store.getRating())
                .userRatingsTotal(store.getUserRatingsTotal())
                .photoUrl(store.getPhotoUrl())
                .videoUrl(store.getVideoUrl())
                .types(store.getTypes() != null ? store.getTypes() : new ArrayList<>())
                .openTime(openTime)
                .closeTime(closeTime)
                .build();
        }
        
        Store saved = storeRepository.save(savedStore);
        log.debug("Store 저장/업데이트 완료: {} (ID: {}, place_id: {})", 
            saved.getName(), saved.getId(), saved.getPlaceId());
        return saved;
    }
    
    /**
     * 리뷰를 DB에 저장합니다 (중복 제거).
     */
    private int saveReviewsToDatabase(Store store, List<Review> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            return 0;
        }
        
        // store가 아직 저장되지 않았을 수 있으므로 ID 확인
        if (store.getId() == null) {
            log.warn("Store ID가 null입니다. 리뷰를 저장할 수 없습니다. (상점: {})", store.getName());
            return 0;
        }
        
        int savedCount = 0;
        for (Review review : reviews) {
            try {
                // 중복 체크 (store_id와 timestamp로)
                if (!reviewRepository.existsByStoreIdAndTimestamp(store.getId(), review.getTimestamp())) {
                    Review savedReview = Review.builder()
                        .store(store)
                        .text(review.getText())
                        .rating(review.getRating())
                        .authorName(review.getAuthorName())
                        .timestamp(review.getTimestamp())
                        .imageUrls(review.getImageUrls() != null ? review.getImageUrls() : new ArrayList<>())
                        .build();
                    
                    reviewRepository.save(savedReview);
                    savedCount++;
                }
            } catch (Exception e) {
                log.warn("리뷰 저장 실패: {} (상점: {})", e.getMessage(), store.getName());
            }
        }
        
        return savedCount;
    }
    
    /**
     * 상점 수집 결과
     */
    public record StoreCollectionResult(
            int totalStores,
            int savedCount,
            int updatedCount,
            int reviewCount
    ) {
    }
    
    /**
     * 장소의 상세 정보와 리뷰를 가져와 Store 엔티티로 변환합니다.
     */
    private Store getStoreDetails(String placeId) {
        try {
            String detailsUrl = String.format(
                "https://maps.googleapis.com/maps/api/place/details/json?place_id=%s&fields=place_id,name,formatted_address,geometry,rating,user_ratings_total,photos,types,reviews,opening_hours&key=%s",
                placeId, googlePlacesApiKey
            );
            
            log.info("Place Details API 호출: place_id={}", placeId);
            log.debug("API URL: {}", detailsUrl);
            
            // 먼저 응답을 String으로 받아서 확인
            String responseBody = null;
            PlaceDetailsResponse detailsResponse = null;
            
            try {
                responseBody = restClient.get()
                        .uri(detailsUrl)
                        .retrieve()
                        .body(String.class);
                
                log.debug("Place Details API 응답 (첫 500자): {}", 
                    responseBody != null && responseBody.length() > 500 ? 
                        responseBody.substring(0, 500) + "..." : responseBody);
                
                // JSON 파싱 (null 필드 무시)
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
                detailsResponse = mapper.readValue(responseBody, PlaceDetailsResponse.class);
                
            } catch (Exception e) {
                log.error("Place Details API 응답 파싱 실패 (place_id: {}): {}", placeId, e.getMessage(), e);
                if (responseBody != null) {
                    log.error("응답 본문 (첫 1000자): {}", 
                        responseBody.length() > 1000 ? responseBody.substring(0, 1000) + "..." : responseBody);
                }
                return null;
            }
            
            if (detailsResponse == null) {
                log.warn("Place Details API 응답이 null입니다. place_id: {}", placeId);
                return null;
            }
            
            if (!"OK".equals(detailsResponse.status())) {
                String errorMsg = detailsResponse.error_message();
                log.error("Place Details API 오류 - 상태: {}, 메시지: {}, place_id: {}", 
                    detailsResponse.status(), errorMsg != null ? errorMsg : "없음", placeId);
                if (responseBody != null) {
                    log.error("응답 본문: {}", responseBody);
                }
                return null;
            }
            
            if (detailsResponse.result() == null) {
                log.warn("Place Details API result가 null입니다. place_id: {}", placeId);
                return null;
            }
            
            PlaceDetails result = detailsResponse.result();
            
            // 위치 정보 추출 및 null 체크
            if (result.geometry() == null || result.geometry().location() == null) {
                log.warn("장소 위치 정보가 없습니다. place_id: {}", placeId);
                return null;
            }
            
            Double latObj = result.geometry().location().lat();
            Double lngObj = result.geometry().location().lng();
            
            if (latObj == null || lngObj == null) {
                log.warn("위도/경도가 null입니다. place_id: {}", placeId);
                return null;
            }
            
            double lat = latObj;
            double lng = lngObj;
            
            // 사진 개수 확인 및 대표 사진 URL 추출
            int photoCount = result.photos() != null ? result.photos().size() : 0;
            String photoUrl = null;
            
            if (photoCount > 0) {
                String photoReference = result.photos().get(0).photo_reference();
                photoUrl = String.format(
                    "https://maps.googleapis.com/maps/api/place/photo?maxwidth=800&photoreference=%s&key=%s",
                    photoReference, googlePlacesApiKey
                );
            }
            
            // 리뷰 개수 확인
            int reviewCount = result.reviews() != null ? result.reviews().size() : 0;
            
            // 최소 요구사항 확인 (리뷰 5개 이상, 사진 10개 이상)
            if (reviewCount < MIN_REVIEW_COUNT || photoCount < MIN_PHOTO_COUNT) {
                log.info("상점 '{}'이(가) 최소 요구사항을 만족하지 않음 - 리뷰: {}개 (필요: {}개), 사진: {}개 (필요: {}개)",
                    result.name(), reviewCount, MIN_REVIEW_COUNT, photoCount, MIN_PHOTO_COUNT);
                return null;
            }
            
            log.debug("상점 '{}' 최소 요구사항 충족 - 리뷰: {}개, 사진: {}개", result.name(), reviewCount, photoCount);
            
            // 영업 시간 파싱 (월~일 순서로 배열 생성)
            // 파싱 실패 시에도 Store 저장을 위해 null 배열로 설정
            // List.of()는 null을 허용하지 않으므로 직접 null을 추가하는 방식 사용
            List<String> openTime = createNullTimeList();
            List<String> closeTime = createNullTimeList();
            
            try {
                openTime = parseOpeningHours(result.opening_hours(), true);
                closeTime = parseOpeningHours(result.opening_hours(), false);
            } catch (Exception e) {
                log.warn("영업 시간 파싱 실패: {}, null 배열로 설정", e.getMessage(), e);
                // 이미 null 배열로 초기화되어 있으므로 그대로 사용
            }
            
            // Store 엔티티 생성 및 리뷰 연결을 try-catch로 감싸서 예외 발생 시에도 처리
            try {
                // Store 엔티티 생성 (리뷰는 나중에 추가)
                Store store = Store.builder()
                        .placeId(result.place_id())
                        .name(result.name())
                        .address(result.formatted_address())
                        .latitude(lat)
                        .longitude(lng)
                        .rating(result.rating())
                        .userRatingsTotal(result.user_ratings_total())
                        .photoUrl(photoUrl)
                        .types(result.types() != null ? result.types() : new ArrayList<>())
                        .openTime(openTime)
                        .closeTime(closeTime)
                        .build();

                log.debug("Store 엔티티 생성 성공: {}", store.getName());
                
                // 리뷰 데이터 추출 및 Review 엔티티 생성
                List<Review> reviews = new ArrayList<>();
                
                // 장소의 photos를 리뷰 이미지로 사용 (Google Places API는 리뷰별 사진을 제공하지 않음)
                List<String> placePhotoUrls = new ArrayList<>();
                if (result.photos() != null && !result.photos().isEmpty()) {
                    log.info("상점 '{}' photos 객체 개수: {}", result.name(), result.photos().size());
                    for (Photo photo : result.photos()) {
                        try {
                            if (photo.photo_reference() != null && !photo.photo_reference().isEmpty()) {
                                String imageUrl = String.format(
                                    "https://maps.googleapis.com/maps/api/place/photo?maxwidth=800&photoreference=%s&key=%s",
                                    photo.photo_reference(), googlePlacesApiKey
                                );
                                placePhotoUrls.add(imageUrl);
                                log.debug("이미지 URL 생성: {}", imageUrl);
                            } else {
                                log.warn("photo_reference가 null이거나 비어있습니다");
                            }
                        } catch (Exception e) {
                            log.error("장소 이미지 URL 생성 실패: {}", e.getMessage(), e);
                        }
                    }
                } else {
                    log.warn("상점 '{}' photos가 null이거나 비어있습니다", result.name());
                }
                
                log.info("상점 '{}' 생성된 사진 URL 개수: {}, 리뷰 개수: {}", 
                    result.name(), placePhotoUrls.size(), 
                    result.reviews() != null ? result.reviews().size() : 0);
                
                if (!placePhotoUrls.isEmpty()) {
                    log.info("첫 번째 이미지 URL 샘플: {}", placePhotoUrls.get(0));
                }
                
                if (result.reviews() != null) {
                    int reviewIndex = 0;
                    for (PlaceReview placeReview : result.reviews()) {
                        try {
                            // 리뷰 이미지 URL 수집
                            List<String> reviewImageUrls = new ArrayList<>();
                            
                            // 1. 먼저 리뷰에 직접 포함된 photos 확인 (있는 경우)
                            if (placeReview.photos() != null && !placeReview.photos().isEmpty()) {
                                log.info("리뷰에 사진이 포함되어 있음: {} 개", placeReview.photos().size());
                                for (ReviewPhoto reviewPhoto : placeReview.photos()) {
                                    try {
                                        String imageUrl = String.format(
                                            "https://maps.googleapis.com/maps/api/place/photo?maxwidth=800&photoreference=%s&key=%s",
                                            reviewPhoto.photo_reference(), googlePlacesApiKey
                                        );
                                        reviewImageUrls.add(imageUrl);
                                    } catch (Exception e) {
                                        log.debug("리뷰 이미지 URL 생성 실패: {}", e.getMessage());
                                    }
                                }
                            }
                            
                            // 2. 리뷰에 사진이 없으면 장소 사진을 분배 (최대 3개)
                            if (reviewImageUrls.isEmpty() && !placePhotoUrls.isEmpty()) {
                                // 각 리뷰에 장소 사진을 순환하며 할당 (최대 3개)
                                int photosPerReview = Math.min(3, placePhotoUrls.size());
                                log.info("리뷰 '{}' - 장소 사진 할당 시작: photosPerReview={}, 전체 사진 개수={}", 
                                    placeReview.author_name(), photosPerReview, placePhotoUrls.size());
                                
                                for (int i = 0; i < photosPerReview; i++) {
                                    int photoIndex = (reviewIndex * photosPerReview + i) % placePhotoUrls.size();
                                    String assignedImageUrl = placePhotoUrls.get(photoIndex);
                                    reviewImageUrls.add(assignedImageUrl);
                                    log.debug("이미지 {} 할당: {}", i+1, assignedImageUrl);
                                }
                                log.info("리뷰 '{}' - 장소 사진 {} 개 할당 완료", 
                                    placeReview.author_name(), reviewImageUrls.size());
                            } else if (reviewImageUrls.isEmpty()) {
                                log.warn("리뷰 '{}' - 할당할 사진이 없습니다 (placePhotoUrls 비어있음)", 
                                    placeReview.author_name());
                            }
                            
                            Review reviewEntity = Review.builder()
                                    .store(store)
                                    .text(placeReview.text())
                                    .rating(placeReview.rating())
                                    .authorName(placeReview.author_name())
                                    .timestamp(placeReview.time())
                                    .imageUrls(reviewImageUrls)
                                    .build();
                            reviews.add(reviewEntity);
                            
                            log.debug("리뷰 생성 완료: 작성자={}, 이미지 개수={}", 
                                placeReview.author_name(), reviewImageUrls.size());
                            
                            reviewIndex++;
                        } catch (Exception e) {
                            log.warn("리뷰 엔티티 생성 실패: {}", e.getMessage());
                            // 개별 리뷰 생성 실패는 무시하고 계속 진행
                        }
                    }
                }
                
                // Store에 리뷰 연결
                store.getReviews().addAll(reviews);
                
                // 비디오 생성은 collectStores 메서드에서 모든 상점 수집 완료 후 일괄 처리
                // 여기서는 videoUrl을 null로 설정
                store.setVideoUrl(null);
                
                return store;
            } catch (Exception e) {
                log.error("Store 엔티티 생성 중 예외 발생: {}", e.getMessage(), e);
                // Store 생성 실패 시 null 반환 (최소 요구사항은 충족했지만 엔티티 생성 실패)
                return null;
            }
            
        } catch (RestClientResponseException e) {
            log.error("상점 상세 정보 조회 실패 - HTTP {}: {}", 
                e.getStatusCode().value(), e.getResponseBodyAsString(), e);
            return null;
        } catch (RestClientException e) {
            log.error("상점 상세 정보 조회 실패", e);
            return null;
        }
    }
    
    /**
     * 장소가 음식점 또는 상점 관련 타입인지 확인합니다.
     */
    private boolean isStoreType(PlaceResult place) {
        if (place.types() == null || place.types().isEmpty()) {
            return false;
        }
        
        return place.types().stream()
                .anyMatch(type -> STORE_TYPES.contains(type));
    }
    
    /**
     * null로 채워진 7개 요소의 리스트를 생성합니다 (월~일 순서).
     * List.of()는 null을 허용하지 않으므로 직접 생성합니다.
     */
    private List<String> createNullTimeList() {
        List<String> times = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            times.add(null);
        }
        return times;
    }
    
    /**
     * Google Places API의 opening_hours를 파싱하여 월~일 순서로 시간 배열을 생성합니다.
     * 파싱 실패 시 null 배열을 반환합니다.
     * 
     * @param openingHours opening_hours 객체 (null 가능)
     * @param isOpenTime true면 오픈 시간, false면 클로즈 시간
     * @return 월~일 순서의 시간 배열 (7개 요소, 영업하지 않는 요일은 null)
     */
    private List<String> parseOpeningHours(OpeningHours openingHours, boolean isOpenTime) {
        // 월~일 순서로 초기화 (모두 null)
        List<String> times = createNullTimeList();
        
        try {
            if (openingHours == null || openingHours.periods() == null || openingHours.periods().isEmpty()) {
                return times;
            }
            
            // Google Places API의 day: 0=Sunday, 1=Monday, ..., 6=Saturday
            // 우리는 월~일 순서로 저장하므로: 인덱스 0=Monday(day=1), 1=Tuesday(day=2), ..., 6=Sunday(day=0)
            for (Period period : openingHours.periods()) {
                try {
                    TimeInfo timeInfo = isOpenTime ? period.open() : period.close();
                    
                    if (timeInfo != null) {
                        Integer day = timeInfo.day();
                        String timeStr = timeInfo.time(); // "1100" 형식
                        
                        // day나 timeStr이 null이면 건너뛰기
                        if (day == null || timeStr == null) {
                            continue;
                        }
                        
                        // "1100" -> "11:00" 형식으로 변환
                        if (timeStr.length() == 4) {
                            String formattedTime = timeStr.substring(0, 2) + ":" + timeStr.substring(2, 4);
                            
                            // day를 월~일 인덱스로 변환
                            // day=0(Sunday) -> 인덱스 6
                            // day=1(Monday) -> 인덱스 0
                            // day=2(Tuesday) -> 인덱스 1
                            // ...
                            // day=6(Saturday) -> 인덱스 5
                            int index = (day == 0) ? 6 : (day - 1);
                            
                            // 같은 요일에 이미 값이 없을 때만 설정 (첫 번째 period 우선)
                            if (index >= 0 && index < 7 && times.get(index) == null) {
                                times.set(index, formattedTime);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.debug("period 파싱 중 오류 발생 (무시하고 계속 진행): {}", e.getMessage());
                    // 개별 period 파싱 실패는 무시하고 계속 진행
                }
            }
        } catch (Exception e) {
            log.warn("영업 시간 파싱 중 오류 발생: {}, null 배열 반환", e.getMessage());
            // 예외 발생 시 null 배열 반환
            return createNullTimeList();
        }
        
        return times;
    }
    
    // Google Places API 응답 DTO
    private record PlacesApiResponse(
            List<PlaceResult> results,
            String status,
            String error_message
    ) {
    }
    
    private record PlaceResult(
            String place_id,
            String name,
            Geometry geometry,
            List<String> types
    ) {
    }
    
    private record Geometry(
            Location location
    ) {
    }
    
    private record Location(
            Double lat,
            Double lng
    ) {
    }
    
    private record PlaceDetailsResponse(
            PlaceDetails result,
            String status,
            String error_message
    ) {
    }
    
    private record PlaceDetails(
            String place_id,
            String name,
            String formatted_address,
            Geometry geometry,
            Double rating,
            Integer user_ratings_total,
            List<Photo> photos,
            List<String> types,
            List<PlaceReview> reviews,
            OpeningHours opening_hours
    ) {
    }
    
    private record PlaceReview(
            String text,
            Integer rating,
            String author_name,
            Long time,
            List<ReviewPhoto> photos  // 리뷰에 첨부된 사진들
    ) {
    }
    
    private record ReviewPhoto(
            String photo_reference,
            Integer height,
            Integer width
    ) {
    }
    
    private record Photo(
            String photo_reference
    ) {
    }
    
    private record OpeningHours(
            List<Period> periods
    ) {
    }
    
    private record Period(
            TimeInfo open,
            TimeInfo close
    ) {
    }
    
    private record TimeInfo(
            Integer day,  // 0=Sunday, 1=Monday, ..., 6=Saturday
            String time   // "1100" 형식 (HHMM)
    ) {
    }
}

