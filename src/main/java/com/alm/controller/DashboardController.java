package com.alm.controller;

import com.alm.dto.DashboardSummaryDTO;
import com.alm.service.DashboardService;
import com.alm.util.MarketDataScheduler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 대시보드 REST API.
 *
 * [API 목록]
 *   GET /api/dashboard/summary  총 자산, 이달 수입/지출/순수지
 *   GET /api/dashboard/market   시장 지수 (MarketDataScheduler 캐시 반환)
 */
@RestController
@RequestMapping("/api/dashboard")
public class
DashboardController {

    private final DashboardService dashboardService = new DashboardService();

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryDTO> getDashboardSummary() {
        return ResponseEntity.ok(dashboardService.aggregateDashboardData());
    }

    /**
     * 시장 지수 캐시 반환. KIS API 직접 호출 없음.
     * MarketDataScheduler 가 1초마다 갱신한 메모리 캐시를 읽어 응답.
     */
    @GetMapping("/market")
    public ResponseEntity<?> getMarketIndices() {
        return ResponseEntity.ok(MarketDataScheduler.getCached());
    }
}
