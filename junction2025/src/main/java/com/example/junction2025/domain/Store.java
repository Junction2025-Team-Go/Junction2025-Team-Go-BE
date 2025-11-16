package com.example.junction2025.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 상점 정보를 저장하는 엔티티 (음식점 및 일반 상점 포함)
 */
@Entity
@Table(name = "stores", indexes = {
    @Index(name = "idx_place_id", columnList = "place_id", unique = true),
    @Index(name = "idx_latitude_longitude", columnList = "latitude,longitude")
})

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Store {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 255)
    private String placeId; // Google Places API의 place_id
    
    @Column(nullable = false, length = 500)
    private String name;
    
    @Column(length = 1000)
    private String address;
    
    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;
    
    private Double rating; // 평점
    
    private Integer userRatingsTotal; // 총 리뷰 수
    
    @Column(length = 1000)
    private String photoUrl; // 대표 사진 URL
    
    @Column(length = 1000)
    private String videoUrl; // Veo 3.1로 생성한 영상 URL (S3)
    
    @ElementCollection
    @CollectionTable(name = "store_types", joinColumns = @JoinColumn(name = "store_id"))
    @Column(name = "type")
    @Builder.Default
    private List<String> types = new ArrayList<>(); // 상점 타입들 (음식점, 일반 상점 등)
    
    // 영업 시간 (월~일 순서, 7개 요소, null 가능)
    @ElementCollection
    @CollectionTable(name = "store_open_times", joinColumns = @JoinColumn(name = "store_id"))
    @Column(name = "open_time", nullable = true, length = 10)
    @Builder.Default
    private List<String> openTime = new ArrayList<>(); // 월~일 오픈 시간 배열 ["11:00", null, "11:00", ...]
    
    @ElementCollection
    @CollectionTable(name = "store_close_times", joinColumns = @JoinColumn(name = "store_id"))
    @Column(name = "close_time", nullable = true, length = 10)
    @Builder.Default
    private List<String> closeTime = new ArrayList<>(); // 월~일 클로즈 시간 배열 ["14:00", null, "14:00", ...]
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "store", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Review> reviews = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public void updateInfo(String name, String address, Double rating, Integer userRatingsTotal, 
                          String photoUrl, List<String> types) {
        this.name = name;
        this.address = address;
        this.rating = rating;
        this.userRatingsTotal = userRatingsTotal;
        this.photoUrl = photoUrl;
        this.types = types != null ? types : new ArrayList<>();
    }
    
    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }
}

