package com.alm.controller;

import com.alm.service.InvestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 투자 포트폴리오 REST API.
 *
 * [API 목록]
 *   GET    /api/invest/portfolio       보유 종목 + KIS 현재가
 *   GET    /api/invest/summary         포트폴리오 요약 (대시보드용)
 *   GET    /api/invest/brokerages      증권위탁계좌 드롭다운
 *   POST   /api/invest/buy             매수 등록
 *   POST   /api/invest/sell            매도 등록
 *   GET    /api/invest/logs            매매 일지 전체 조회
 *   DELETE /api/invest/logs/{id}       매매 이력 삭제
 *   GET    /api/invest/stocks/lookup   KIS API 종목 실시간 조회
 */
@RestController
@RequestMapping("/api/invest")
public class InvestController {

    private final InvestService investService = new InvestService();

    @GetMapping("/portfolio")
    public ResponseEntity<?> getPortfolio() {
        try {
            return ResponseEntity.ok(investService.getPortfolioWithPrices());
        } catch (Exception e) {
            return ResponseEntity.ok(List.of());
        }
    }

    @GetMapping("/summary")
    public ResponseEntity<?> getSummary() {
        try {
            return ResponseEntity.ok(investService.getPortfolioSummary());
        } catch (Exception e) {
            return ResponseEntity.ok(
                Map.of("totalBook", 0, "totalCurrent", 0,
                       "totalProfit", 0, "profitRate", 0.0, "holdingCount", 0)
            );
        }
    }

    @GetMapping("/brokerages")
    public ResponseEntity<?> getBrokerages() {
        try {
            return ResponseEntity.ok(investService.getBrokerageAccounts());
        } catch (Exception e) {
            return ResponseEntity.ok(List.of());
        }
    }

    @PostMapping("/buy")
    public ResponseEntity<?> buy(@RequestBody Map<String, Object> payload) {
        try {
            investService.buyStock(payload);
            return ResponseEntity.ok(Map.of("message", "매수가 등록되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/sell")
    public ResponseEntity<?> sell(@RequestBody Map<String, Object> payload) {
        try {
            investService.sellStock(payload);
            return ResponseEntity.ok(Map.of("message", "매도가 등록되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/logs")
    public ResponseEntity<?> getLogs() {
        try {
            return ResponseEntity.ok(investService.getLogs());
        } catch (Exception e) {
            return ResponseEntity.ok(List.of());
        }
    }

    @DeleteMapping("/logs/{id}")
    public ResponseEntity<?> deleteLog(@PathVariable int id) {
        try {
            investService.deleteLog(id);
            return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/stocks/lookup")
    public ResponseEntity<?> lookupStock(@RequestParam String code) {
        try {
            return ResponseEntity.ok(investService.lookupStock(code));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
