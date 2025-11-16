package com.example.junction2025.repository;

import com.example.junction2025.domain.Support;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface SupportRepository extends JpaRepository<Support, Long> {
    
    /**
     * 첫 번째 Support 엔티티를 조회합니다 (비관적 락).
     * 동시성 문제를 방지하기 위해 PESSIMISTIC_WRITE 락을 사용합니다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Support> findFirstByOrderByIdAsc();
    
    /**
     * 첫 번째 Support 엔티티를 조회합니다 (읽기 전용).
     */
    Optional<Support> findTopByOrderByIdAsc();
}

