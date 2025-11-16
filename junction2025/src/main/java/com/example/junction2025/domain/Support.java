package com.example.junction2025.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 서포터 카운트를 저장하는 엔티티
 * DB에는 단 하나의 레코드만 존재합니다.
 */
@Entity
@Table(name = "support")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Support {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Integer count;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (count == null) {
            count = 0;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * 카운트를 1 증가시킵니다.
     */
    public void incrementCount() {
        this.count++;
    }
    
    /**
     * 초기 Support 엔티티를 생성합니다.
     */
    public static Support createInitial() {
        Support support = new Support();
        support.count = 0;
        return support;
    }
}

