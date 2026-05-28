package com.alm.dto;

/**
 * 대시보드 요약 집계 DTO.
 *
 * [집계 항목]
 *   totalAsset      : 전체 자산(ACC+REA+PHY+CSH) 합계
 *   monthlyIncome   : 이달 amount > 0 항목 SUM
 *   monthlyExpense  : 이달 amount < 0 항목 ABS(SUM) — 항상 양수
 *   monthlyNet      : monthlyIncome - monthlyExpense (양수=흑자, 음수=적자)
 *
 * [Map 대신 DTO 사용 이유]
 *   필드가 명확히 문서화되고, 컴파일 시 타입 체크 및 IDE 자동완성이 지원된다.
 */
public class DashboardSummaryDTO {

    private long totalAsset;
    private long monthlyIncome;
    private long monthlyExpense;
    private long monthlyNet;

    public long getTotalAsset()                    { return totalAsset; }
    public void setTotalAsset(long totalAsset)     { this.totalAsset = totalAsset; }

    public long getMonthlyIncome()                     { return monthlyIncome; }
    public void setMonthlyIncome(long monthlyIncome)   { this.monthlyIncome = monthlyIncome; }

    public long getMonthlyExpense()                      { return monthlyExpense; }
    public void setMonthlyExpense(long monthlyExpense)   { this.monthlyExpense = monthlyExpense; }

    public long getMonthlyNet()                    { return monthlyNet; }
    public void setMonthlyNet(long monthlyNet)     { this.monthlyNet = monthlyNet; }
}
