package com.alm.dto;

/**
 * [DTO 계층 - 대시보드 요약 정보]
 * 메인 화면(main.html) 대시보드에 표시할 집계 데이터를 담는 DTO.
 * 기획서 5.7절에 정의된 클래스.
 *
 * [대시보드 정보 구성]
 *   - 총 자산: 전체 자산(ACC+REA+PHY+CSH)의 합계
 *   - 이달 수입: 이번 월 general_ledger 중 amount > 0 합계
 *   - 이달 지출: 이번 월 general_ledger 중 amount < 0 합계 (절댓값)
 *   - 이달 순수지: 수입 - 지출
 *
 * [Java 문법 개념] 단순 데이터 홀더(POJO):
 *   - Plain Old Java Object: 특정 프레임워크 규약 없이 필드 + getter/setter만 있는 클래스.
 *   - 상속 없음, 인터페이스 구현 없음 → 가장 단순한 Java 클래스 형태.
 *   - Jackson이 getter 메서드를 스캔하여 JSON 직렬화 수행.
 *     예: getTotalAsset() → {"totalAsset": 15000000} JSON 생성.
 *
 * [설계 의도]
 *   - 여러 테이블(asset_master + general_ledger)에서 집계한 결과를 하나의 응답 객체로 묶음.
 *   - Map<String, Object>로도 처리 가능하지만, DTO를 사용하면:
 *     1. 필드가 명확히 문서화됨.
 *     2. 컴파일 시 타입 체크 가능.
 *     3. IDE의 자동완성 지원.
 */
public class DashboardSummaryDTO {

    // 전체 자산 합계. AssetService.calculateTotalAsset()의 결과.
    // long 타입: 자산 합계는 수억~수십억 범위. int(약 21억 한계)로는 부족할 수 있음.
    private long totalAsset;

    // 이달 수입 합계. amount > 0인 항목들의 SUM.
    private long monthlyIncome;

    // 이달 지출 합계. amount < 0인 항목들의 ABS(SUM). 항상 양수로 표현.
    private long monthlyExpense;

    // 이달 순수지. monthlyIncome - monthlyExpense. 양수면 흑자, 음수면 적자.
    private long monthlyNet;

    // ── Getter / Setter ──────────────────────────────────────────────
    // [Java 문법 개념] 필드명과 getter/setter 이름의 관계:
    //   - 필드명: totalAsset (camelCase)
    //   - getter: getTotalAsset() → Jackson이 JSON key "totalAsset"으로 직렬화
    //   - setter: setTotalAsset(long totalAsset) → JSON 역직렬화 시 호출
    //   - this.totalAsset = totalAsset: 필드(this.로 접근)와 파라미터(같은 이름)를 구분.

    public long getTotalAsset()                   { return totalAsset; }
    public void setTotalAsset(long totalAsset)    { this.totalAsset = totalAsset; }

    public long getMonthlyIncome()                    { return monthlyIncome; }
    public void setMonthlyIncome(long monthlyIncome)  { this.monthlyIncome = monthlyIncome; }

    public long getMonthlyExpense()                     { return monthlyExpense; }
    public void setMonthlyExpense(long monthlyExpense)  { this.monthlyExpense = monthlyExpense; }

    public long getMonthlyNet()                   { return monthlyNet; }
    public void setMonthlyNet(long monthlyNet)    { this.monthlyNet = monthlyNet; }
}
