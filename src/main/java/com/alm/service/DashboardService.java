package com.alm.service;

import com.alm.dto.DashboardSummaryDTO;
import com.alm.repository.LedgerRepository;

/**
 * [Service 계층 - 대시보드 비즈니스 로직]
 * 메인 화면 대시보드에 표시할 집계 데이터를 수집·조합하는 클래스.
 * 기획서 5.7절에 정의된 클래스.
 *
 * [현재 구현 상태]
 *   - 스텁(Stub) 구현. 기획서의 클래스 구조는 완성.
 *   - aggregateDashboardData()의 실제 로직은 AssetService + LedgerService 완전 통합 후 완성 예정.
 *
 * [Java 문법 개념] 스텁 클래스:
 *   - 클래스 선언 + 필드 + 메서드 시그니처는 정의됐지만, 메서드 body가 완전히 구현되지 않은 상태.
 *   - 기획서에 설계된 구조를 코드로 반영해 두어, 향후 구현자가 설계 의도를 파악하기 쉽게 함.
 *   - TODO 주석으로 미완성 부분을 명시.
 */
public class DashboardService {

    // LedgerRepository: 이달 수입/지출 합계 조회에 사용.
    // [Java 문법 개념] 필드 선언과 동시에 초기화 (Eager Initialization):
    //   - 생성자 없이 new LedgerRepository()를 필드 선언부에서 즉시 실행.
    //   - final: 재할당 불가로 참조 안정성 보장.
    private final LedgerRepository ledgerRepository = new LedgerRepository();

    /**
     * [기획서 5.7절] 대시보드에 필요한 모든 집계 데이터를 수집·조합하여 반환.
     *
     * [처리 흐름 설계 (미구현)]
     *   1. AssetService.calculateTotalAsset()   → 총 자산 계산
     *   2. LedgerRepository.getSumByCategory()  → 이달 수입/지출 집계
     *   3. DashboardSummaryDTO에 집계값 세팅
     *   4. 반환
     *
     * [Java 문법 개념] 여러 Service 협력(Collaboration):
     *   - DashboardService는 AssetService와 LedgerService(또는 Repository)를 동시에 참조하여
     *     두 도메인의 데이터를 하나의 DTO로 조합하는 오케스트레이터(Orchestrator) 역할.
     *   - 이처럼 여러 Service를 조합하는 상위 Service 패턴을 Facade Service라고도 부름.
     *
     * @return DashboardSummaryDTO (총자산, 이달 수입, 지출, 순수지 포함)
     */
    public DashboardSummaryDTO aggregateDashboardData() {
        // TODO: 다음 단계 구현 필요
        // 1. AssetService assetService = new AssetService();
        //    long totalAsset = assetService.calculateTotalAsset();
        // 2. Map<String, Long> ledgerSummary = ledgerRepository.getSumByCategory();
        // 3. DashboardSummaryDTO dto = new DashboardSummaryDTO();
        //    dto.setTotalAsset(totalAsset);
        //    dto.setMonthlyIncome(ledgerSummary.getOrDefault("totalIncome", 0L));
        //    dto.setMonthlyExpense(ledgerSummary.getOrDefault("totalExpense", 0L));
        //    dto.setMonthlyNet(ledgerSummary.getOrDefault("net", 0L));
        //    return dto;

        // 현재: 임시 빈 DTO 반환 (스텁)
        // UnsupportedOperationException 대신 빈 DTO 반환 → 프론트엔드 연동 테스트 가능하게 함
        return new DashboardSummaryDTO();
    }
}
