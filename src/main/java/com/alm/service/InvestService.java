package com.alm.service;

import com.alm.dto.InvestLogDTO;
import com.alm.dto.InvestPortfolioDTO;
import com.alm.repository.InvestRepository;
import com.alm.util.KisApiAdapter;
import com.alm.util.ParseUtil;
import com.alm.util.StockInfoProvider;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 투자 포트폴리오 서비스. */
public class InvestService {

    private final InvestRepository  investRepo;
    private final StockInfoProvider stockProvider;

    /** 기본 생성자. */
    public InvestService() {
        this.investRepo    = new InvestRepository();
        this.stockProvider = new KisApiAdapter();
    }

    /** 테스트용 의존성 주입 생성자. */
    public InvestService(InvestRepository investRepo, StockInfoProvider stockProvider) {
        this.investRepo    = investRepo;
        this.stockProvider = stockProvider;
    }

    // ── 포트폴리오 조회 ───────────────────────────────────────────────

    /**
     * 보유 종목 목록 + KIS 현재가 조회.
     * 종목별 API 실패는 api_error=true 로 표시하고 나머지 종목 조회를 계속한다.
     */
    public List<InvestPortfolioDTO> getPortfolioWithPrices() {
        List<InvestPortfolioDTO> list = investRepo.findAllPortfolio();
        for (InvestPortfolioDTO p : list) {
            try {
                String[] info    = stockProvider.getStockInfo(p.getTicker_code());
                long     curPrice = Long.parseLong(info[0]);
                long     curVal   = curPrice * p.getQuantity();
                long     bookVal  = p.getPurchase_price() * p.getQuantity();
                p.setCurrent_price(curPrice);
                p.setPrice_change(info[1]);
                p.setCurrent_value(curVal);
                p.setBook_value(bookVal);
                p.setProfit_loss(curVal - bookVal);
                // Math.round(× 10000) / 100.0 : 소수 둘째 자리 반올림
                p.setProfit_rate(bookVal > 0
                    ? Math.round((double)(curVal - bookVal) / bookVal * 10000.0) / 100.0
                    : 0.0);
            } catch (Exception e) {
                p.setCurrent_price(-1);
                p.setApi_error(true);
            }
        }
        return list;
    }

    /** 포트폴리오 요약 (대시보드용). API 오류 종목은 집계 제외. */
    public Map<String, Object> getPortfolioSummary() {
        List<InvestPortfolioDTO> list = getPortfolioWithPrices();
        long totalBook = 0, totalCurrent = 0;
        for (InvestPortfolioDTO p : list) {
            if (!p.isApi_error()) { totalBook += p.getBook_value(); totalCurrent += p.getCurrent_value(); }
        }
        long   profit     = totalCurrent - totalBook;
        double profitRate = totalBook > 0
            ? Math.round((double) profit / totalBook * 10000.0) / 100.0 : 0.0;
        Map<String, Object> result = new HashMap<>();
        result.put("totalBook",    totalBook);
        result.put("totalCurrent", totalCurrent);
        result.put("totalProfit",  profit);
        result.put("profitRate",   profitRate);
        result.put("holdingCount", list.size());
        return result;
    }

    public List<Map<String, Object>> getBrokerageAccounts() {
        return investRepo.findBrokerageAccounts();
    }

    // ── 매수 ─────────────────────────────────────────────────────────

    /**
     * 매수 처리. 포트폴리오 UPSERT → 매매 이력 INSERT → 계좌 잔액 차감.
     * 종목명 미입력 시 KIS API 에서 자동 조회한다.
     */
    public void buyStock(Map<String, Object> payload) throws Exception {
        int    assetId    = ParseUtil.parseInt(payload.get("asset_id"), 0);
        String tickerCode = getString(payload, "ticker_code");
        String tickerName = getString(payload, "ticker_name", "");
        int    qty        = ParseUtil.parseInt(payload.get("quantity"), 0);
        long   price      = ParseUtil.parseLong(payload.get("price"));
        String reason     = getString(payload, "reason", "");
        String date       = getString(payload, "trade_date", LocalDate.now().toString());

        if (assetId <= 0)         throw new Exception("증권 계좌를 선택하세요.");
        if (tickerCode.isEmpty()) throw new Exception("종목 코드를 입력하세요.");
        if (qty  <= 0)            throw new Exception("수량을 올바르게 입력하세요.");
        if (price <= 0)           throw new Exception("매수가를 올바르게 입력하세요.");

        if (tickerName.isEmpty()) tickerName = tickerCode;

        investRepo.buyStock(assetId, tickerCode, tickerName, qty, price);
        investRepo.insertLog(assetId, tickerCode, tickerName, "BUY", qty, price, 0L, reason, date);
        investRepo.updateAccountBalance(assetId, -(long) qty * price);
    }

    // ── 매도 ─────────────────────────────────────────────────────────

    /**
     * 매도 처리. ticker_code 로 포트폴리오에서 계좌를 자동 조회한다.
     * 잔량 차감 → 매매 이력 INSERT → 계좌 잔액 증가.
     */
    public void sellStock(Map<String, Object> payload) throws Exception {
        String tickerCode = getString(payload, "ticker_code");
        int    qty        = ParseUtil.parseInt(payload.get("quantity"), 0);
        long   price      = ParseUtil.parseLong(payload.get("price"));
        String reason     = getString(payload, "reason", "");
        String date       = getString(payload, "trade_date", LocalDate.now().toString());

        if (tickerCode.isEmpty()) throw new Exception("종목 코드를 입력하세요.");
        if (qty  <= 0)            throw new Exception("수량을 올바르게 입력하세요.");
        if (price <= 0)           throw new Exception("매도가를 올바르게 입력하세요.");

        InvestPortfolioDTO holding = investRepo.findPortfolioByTicker(tickerCode);
        if (holding == null)                  throw new Exception("보유하지 않은 종목입니다.");
        if (holding.getQuantity() < qty)      throw new Exception("보유 수량(" + holding.getQuantity() + "주)보다 많이 매도할 수 없습니다.");

        int  assetId = holding.getAsset_id();
        long profit  = investRepo.sellStock(assetId, tickerCode, qty, price);

        investRepo.insertLog(assetId, tickerCode, holding.getTicker_name(), "SELL", qty, price, profit, reason, date);
        investRepo.updateAccountBalance(assetId, (long) qty * price);
    }

    // ── 이력 조회 / 삭제 ─────────────────────────────────────────────

    public List<InvestLogDTO> getLogs()  { return investRepo.findAllLogs(); }
    public void deleteLog(int logId)     { investRepo.deleteLog(logId); }

    // ── 파싱 헬퍼 ────────────────────────────────────────────────────

    private String getString(Map<String, Object> m, String k) {
        Object v = m.get(k); return v == null ? "" : v.toString().trim();
    }
    private String getString(Map<String, Object> m, String k, String def) {
        Object v = m.get(k); return (v == null || v.toString().trim().isEmpty()) ? def : v.toString().trim();
    }
}
