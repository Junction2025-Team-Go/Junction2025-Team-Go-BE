package com.example.junction2025.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 상점 리뷰를 저장하는 엔티티 (음식점 및 일반 상점 포함)
 */
@Entity
@Table(name = "reviews", indexes = {
    @Index(name = "idx_store_id", columnList = "store_id"),
    @Index(name = "idx_author_name", columnList = "author_name")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;
    
    @Column(nullable = false, length = 5000)
    private String text; // 리뷰 텍스트
    
    @Column(nullable = false)
    private Integer rating; // 평점 (1-5)
    
    @Column(length = 255)
    private String authorName; // 작성자 이름
    
    @Column(nullable = false)
    private Long timestamp; // Google Places API의 timestamp
    
    // 리뷰 이미지 URL 목록
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "review_images", joinColumns = @JoinColumn(name = "review_id"))
    @Column(name = "image_url", length = 1000)
    private List<String> imageUrls = new ArrayList<>(); // 리뷰 이미지 URL 리스트
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    @Builder
    public Review(Store store, String text, Integer rating, String authorName, Long timestamp, List<String> imageUrls) {
        this.store = store;
        this.text = text;
        this.rating = rating;
        this.authorName = authorName;
        this.timestamp = timestamp;
        this.imageUrls = imageUrls != null ? imageUrls : new ArrayList<>();
    }
    
    public void updateText(String text) {
        this.text = text;
    }
}

