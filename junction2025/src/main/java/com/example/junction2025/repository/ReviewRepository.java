package com.example.junction2025.repository;

import com.example.junction2025.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    
    List<Review> findByStoreId(Long storeId);
    
    @Query("SELECT r FROM Review r WHERE r.store.placeId = :placeId")
    List<Review> findByStorePlaceId(@Param("placeId") String placeId);
    
    boolean existsByStoreIdAndTimestamp(Long storeId, Long timestamp);
    
    // Store ID 목록으로 리뷰 조회 (N+1 문제 방지, JOIN FETCH로 Store도 함께 로딩)
    @Query("SELECT r FROM Review r JOIN FETCH r.store WHERE r.store.id IN :storeIds")
    List<Review> findByStoreIds(@Param("storeIds") List<Long> storeIds);
}

