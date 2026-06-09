package com.alm.dto;

/** 대시보드 요약 집계 DTO. */
public class DashboardSummaryDTO {

    // ── 자산 ────────────────────────────────────────────────
    private long totalAsset;

    // ── 가계부 (이달) ────────────────────────────────────────
    private long monthlyIncome;
    private long monthlyExpense;
    private long monthlyNet;

    // ── 포트폴리오 ───────────────────────────────────────────
    private long   portfolioBook;        // 매입 평가액 합계 (quantity × purchase_price)
    private long   portfolioProfit;      // 평가 손익 (현재가 - 매입가)
    private double portfolioProfitRate;  // 수익률 (%)
    private int    portfolioCount;       // 보유 종목 수

    // ── 목표 ─────────────────────────────────────────────────
    private int goalCount;           // 전체 등록 목표 수
    private int avgAchievementRate;  // 평균 달성률 (%)

    // ── Getters / Setters ────────────────────────────────────

    public long getTotalAsset()                       { return totalAsset; }
    public void setTotalAsset(long totalAsset)        { this.totalAsset = totalAsset; }

    public long getMonthlyIncome()                          { return monthlyIncome; }
    public void setMonthlyIncome(long monthlyIncome)        { this.monthlyIncome = monthlyIncome; }

    public long getMonthlyExpense()                           { return monthlyExpense; }
    public void setMonthlyExpense(long monthlyExpense)        { this.monthlyExpense = monthlyExpense; }

    public long getMonthlyNet()                       { return monthlyNet; }
    public void setMonthlyNet(long monthlyNet)        { this.monthlyNet = monthlyNet; }

    public long   getPortfolioBook()                              { return portfolioBook; }
    public void   setPortfolioBook(long portfolioBook)            { this.portfolioBook = portfolioBook; }

    public long   getPortfolioProfit()                            { return portfolioProfit; }
    public void   setPortfolioProfit(long portfolioProfit)        { this.portfolioProfit = portfolioProfit; }

    public double getPortfolioProfitRate()                              { return portfolioProfitRate; }
    public void   setPortfolioProfitRate(double portfolioProfitRate)    { this.portfolioProfitRate = portfolioProfitRate; }

    public int  getPortfolioCount()                       { return portfolioCount; }
    public void setPortfolioCount(int portfolioCount)     { this.portfolioCount = portfolioCount; }

    public int  getGoalCount()                    { return goalCount; }
    public void setGoalCount(int goalCount)       { this.goalCount = goalCount; }

    public int  getAvgAchievementRate()                         { return avgAchievementRate; }
    public void setAvgAchievementRate(int avgAchievementRate)   { this.avgAchievementRate = avgAchievementRate; }
}
