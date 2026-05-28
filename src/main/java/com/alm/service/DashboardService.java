package com.alm.service;

import com.alm.dto.DashboardSummaryDTO;
import com.alm.repository.LedgerRepository;

/**
 * 대시보드 집계 서비스.
 *
 * [미구현 — 스텁]
 *   AssetService + LedgerService 통합 후 완성 예정.
 *   현재는 빈 DTO 반환 (프론트엔드 연동 테스트 가능하도록 UnsupportedOperationException 대신 사용).
 *
 * [설계 의도]
 *   여러 도메인 Service 의 데이터를 하나의 DTO 로 조합하는 Facade 역할.
 *   처리 순서: calculateTotalAsset() → getSumByCategory() → DTO 조립 → 반환.
 */
public class DashboardService {

    private final LedgerRepository ledgerRepository = new LedgerRepository();

    public DashboardSummaryDTO aggregateDashboardData() {
        // TODO: AssetService.calculateTotalAsset() + LedgerRepository.getSumByCategory() 연동
        return new DashboardSummaryDTO();
    }
}
