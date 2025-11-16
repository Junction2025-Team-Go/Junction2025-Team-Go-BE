package com.example.junction2025.services;

import com.example.junction2025.domain.Support;
import com.example.junction2025.repository.SupportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 서포터 카운트 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SupportService {
    
    private final SupportRepository supportRepository;
    
    /**
     * 서포터 카운트를 1 증가시킵니다.
     * 동시성 문제를 방지하기 위해 비관적 락을 사용합니다.
     *
     * @return 증가된 후의 총 카운트
     */
    @Transactional
    public int increaseSupportCount() {
        // 비관적 락을 사용하여 Support 엔티티 조회
        Support support = supportRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> {
                    // 첫 실행 시 Support 엔티티 생성
                    log.info("Support 엔티티가 존재하지 않아 새로 생성합니다.");
                    Support newSupport = Support.createInitial();
                    return supportRepository.save(newSupport);
                });
        
        // 카운트 증가
        support.incrementCount();
        supportRepository.save(support);
        
        int newCount = support.getCount();
        log.info("서포터 카운트 증가: {}", newCount);
        
        return newCount;
    }
    
    /**
     * 현재 서포터 카운트를 조회합니다.
     *
     * @return 현재 총 카운트
     */
    @Transactional(readOnly = true)
    public int getSupportCount() {
        Support support = supportRepository.findTopByOrderByIdAsc()
                .orElseGet(() -> {
                    log.debug("Support 엔티티가 존재하지 않습니다. 0을 반환합니다.");
                    return Support.createInitial();
                });
        
        return support.getCount();
    }
}

