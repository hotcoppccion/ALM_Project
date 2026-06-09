package com.alm.service;

import com.alm.dto.DashboardSummaryDTO;
import com.alm.dto.GoalDTO;
import com.alm.repository.LedgerRepository;

import java.util.List;
import java.util.Map;

/**
 * 대시보드 집계 서비스.
 * 자산·가계부·포트폴리오·목표 데이터를 하나의 DTO 로 조합해 반환한다.
 * KIS API 오류 시 portfolioProfit / portfolioProfitRate 는 0 으로 유지된다.
 */
public class DashboardService {

    private final AssetService     assetService;
    private final LedgerRepository ledgerRepository;
    private final InvestService    investService;
    private final GoalService      goalService;

    /** 기본 생성자. */
    public DashboardService() {
        this.assetService     = new AssetService();
        this.ledgerRepository = new LedgerRepository();
        this.investService    = new InvestService();
        this.goalService      = new GoalService();
    }

    /** 테스트용 의존성 주입 생성자. */
    public DashboardService(AssetService assetService,
                            LedgerRepository ledgerRepository,
                            InvestService investService,
                            GoalService goalService) {
        this.assetService     = assetService;
        this.ledgerRepository = ledgerRepository;
        this.investService    = investService;
        this.goalService      = goalService;
    }

    public DashboardSummaryDTO aggregateDashboardData() {
        DashboardSummaryDTO dto = new DashboardSummaryDTO();

        // 전체 자산 합계 (ACC + REA + PHY + CSH + 포트폴리오 매입가)
        dto.setTotalAsset(assetService.calculateTotalAsset());

        // 이달 수입 / 지출 / 순수지
        Map<String, Long> monthly = ledgerRepository.getSumByCategory();
        dto.setMonthlyIncome  (monthly.getOrDefault("totalIncome",  0L));
        dto.setMonthlyExpense (monthly.getOrDefault("totalExpense", 0L));
        dto.setMonthlyNet     (monthly.getOrDefault("net",          0L));

        // 포트폴리오 평가 손익 (KIS API 실시간 조회)
        Map<String, Object> ps = investService.getPortfolioSummary();
        dto.setPortfolioBook      ((Long)    ps.getOrDefault("totalBook",    0L));
        dto.setPortfolioProfit    ((Long)    ps.getOrDefault("totalProfit",  0L));
        dto.setPortfolioProfitRate((Double)  ps.getOrDefault("profitRate",   0.0));
        dto.setPortfolioCount     ((Integer) ps.getOrDefault("holdingCount", 0));

        // 목표 달성률 집계
        List<GoalDTO> goals = goalService.getGoalsWithProgress();
        dto.setGoalCount(goals.size());
        if (!goals.isEmpty()) {
            int avgRate = (int) goals.stream()
                    .mapToInt(GoalDTO::getAchievement_rate)
                    .average()
                    .orElse(0.0);
            dto.setAvgAchievementRate(avgRate);
        }

        return dto;
    }
}
