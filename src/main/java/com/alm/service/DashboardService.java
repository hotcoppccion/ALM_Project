package com.alm.service;

import com.alm.dto.DashboardSummaryDTO;
import com.alm.repository.LedgerRepository;
import java.util.Map;

/**
 * 대시보드 집계 서비스 (Facade).
 * 여러 도메인 데이터를 하나의 DTO 로 조합해 반환한다.
 */
public class DashboardService {

    private final AssetService     assetService     = new AssetService();
    private final LedgerRepository ledgerRepository = new LedgerRepository();

    public DashboardSummaryDTO aggregateDashboardData() {
        DashboardSummaryDTO dto = new DashboardSummaryDTO();

        // 전체 자산 합계 (ACC + REA + PHY + CSH)
        dto.setTotalAsset(assetService.calculateTotalAsset());

        // 이달 수입 / 지출 / 순수지
        Map<String, Long> monthly = ledgerRepository.getSumByCategory();
        dto.setMonthlyIncome  (monthly.getOrDefault("totalIncome",  0L));
        dto.setMonthlyExpense (monthly.getOrDefault("totalExpense", 0L));
        dto.setMonthlyNet     (monthly.getOrDefault("net",          0L));

        return dto;
    }
}
