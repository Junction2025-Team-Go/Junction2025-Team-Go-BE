package com.example.junction2025.services;

import com.example.junction2025.utils.ReviewParseManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 위치 기반 상점 데이터 수집 서비스 (음식점 및 일반 상점 포함)
 * <p>
 * 위치를 격자로 나누어 반경 20km 내의 모든 상점을 수집합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceCollectionService {
    
    private static final double RADIUS_KM = 10.0;
    private static final double GRID_SPACING_KM = 10.0; // 격자 간격 (3km로 설정)
    private static final int BATCH_DELAY_MS = 200; // 배치 간 지연 시간 (최소화)
    
    private final ReviewParseManager reviewParseManager;
    
    /**
     * 중심 위치 기준 반경 20km 내의 모든 상점을 수집하여 DB에 저장합니다 (음식점 및 일반 상점 포함).
     * 위치를 격자로 나누어 여러 번 API를 호출하여 모든 상점을 수집합니다.
     *
     * @param centerLatitude  중심 위도
     * @param centerLongitude 중심 경도
     * @return 수집 통계 정보
     */
    @Transactional
    public CollectionStatistics collectAllStores(double centerLatitude, double centerLongitude) {
        log.info("상점 수집 시작 - 중심 위치: ({}, {})", centerLatitude, centerLongitude);
        
        // 격자 위치 생성
        List<GridPoint> gridPoints = generateGridPoints(centerLatitude, centerLongitude);
        log.info("생성된 격자 포인트 개수: {}", gridPoints.size());
        
        // 통계 변수
        int totalSavedCount = 0;
        int totalUpdatedCount = 0;
        int totalReviewCount = 0;
        Set<String> allProcessedPlaceIds = new HashSet<>();
        
        int processedGrids = 0;
        for (GridPoint gridPoint : gridPoints) {
            try {
                if (processedGrids % 10 == 0) { // 10개마다만 로그 출력
                    log.info("격자 포인트 처리 진행 중... ({}/{})", 
                        ++processedGrids, gridPoints.size());
                } else {
                    processedGrids++;
                }
                
                // 각 격자 포인트에서 상점 수집 및 저장 (ReviewParseManager에서 직접 저장)
                log.debug("격자 포인트 ({}, {})에서 상점 수집 시작", 
                    gridPoint.latitude(), gridPoint.longitude());
                
                com.example.junction2025.utils.ReviewParseManager.StoreCollectionResult result = 
                    reviewParseManager.collectStores(gridPoint.latitude(), gridPoint.longitude());
                
                totalSavedCount += result.savedCount();
                totalUpdatedCount += result.updatedCount();
                totalReviewCount += result.reviewCount();
                
                log.debug("격자 포인트 ({}, {}) 처리 완료 - 상점: {}개, 저장: {}개, 업데이트: {}개", 
                    gridPoint.latitude(), gridPoint.longitude(),
                    result.totalStores(), result.savedCount(), result.updatedCount());
                
                // 배치 간 지연 (최소화)
                TimeUnit.MILLISECONDS.sleep(BATCH_DELAY_MS);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("상점 수집이 중단되었습니다.", e);
                break;
            } catch (Exception e) {
                log.error("격자 포인트 ({}, {}) 처리 중 오류 발생", 
                    gridPoint.latitude(), gridPoint.longitude(), e);
            }
        }
        
        log.info("총 격자 포인트 {}개 처리 완료", gridPoints.size());
        log.info("DB 저장 완료 - 신규: {}개, 업데이트: {}개, 리뷰: {}개", 
            totalSavedCount, totalUpdatedCount, totalReviewCount);
        
        CollectionStatistics statistics = new CollectionStatistics(
            gridPoints.size(),
            totalSavedCount + totalUpdatedCount, // 발견된 상점 수
            totalSavedCount,
            totalUpdatedCount,
            totalReviewCount
        );
        
        log.info("상점 수집 완료 - {}", statistics);
        return statistics;
    }
    
    /**
     * 중심 위치를 기준으로 격자 포인트를 생성합니다.
     * 격자 간격을 12km로 설정하여 20km 반경 내의 모든 상점을 수집할 수 있도록 합니다.
     */
    private List<GridPoint> generateGridPoints(double centerLat, double centerLng) {
        List<GridPoint> gridPoints = new ArrayList<>();
        
        // 위도 1도 ≈ 111km, 경도 1도 ≈ 111km * cos(위도)
        double latStep = GRID_SPACING_KM / 111.0;
        double lngStep = GRID_SPACING_KM / (111.0 * Math.cos(Math.toRadians(centerLat)));
        
        // 격자 범위 계산 (중심에서 ±20km)
        int gridSize = (int) Math.ceil((RADIUS_KM * 2) / GRID_SPACING_KM) + 1;
        
        for (int i = -gridSize; i <= gridSize; i++) {
            for (int j = -gridSize; j <= gridSize; j++) {
                double lat = centerLat + (i * latStep);
                double lng = centerLng + (j * lngStep);
                
                // 중심으로부터 거리 확인 (20km 이내만 포함)
                double distance = calculateDistance(centerLat, centerLng, lat, lng);
                if (distance <= RADIUS_KM) {
                    gridPoints.add(new GridPoint(lat, lng));
                }
            }
        }
        
        return gridPoints;
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
    
    /**
     * 격자 포인트를 나타내는 레코드
     */
    private record GridPoint(double latitude, double longitude) {
    }
    
    /**
     * 수집 통계 정보
     */
    public record CollectionStatistics(
            int gridPointsProcessed,
            int uniqueStoresFound,
            int storesSaved,
            int storesUpdated,
            int reviewsSaved
    ) {
        @Override
        public String toString() {
            return String.format(
                "격자 포인트: %d개, 발견된 상점: %d개, 신규 저장: %d개, 업데이트: %d개, 리뷰 저장: %d개",
                gridPointsProcessed, uniqueStoresFound, storesSaved, storesUpdated, reviewsSaved
            );
        }
    }
}

