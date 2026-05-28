package com.alm.controller;

import com.alm.dto.GeneralLedgerDTO;
import com.alm.service.LedgerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/**
 * 가계부 도메인 REST API.
 *
 * [API 목록]
 *   GET    /api/ledger/list          전체 가계부 내역
 *   GET    /api/ledger/summary       이달 수입/지출/순수지 요약
 *   GET    /api/ledger/categories    카테고리 드롭다운
 *   GET    /api/ledger/liquid-assets 연동 가능 자산(ACC+CSH) 드롭다운
 *   POST   /api/ledger/categories    카테고리 추가
 *   POST   /api/ledger               내역 등록 + 자산 잔액 즉시 반영
 *   DELETE /api/ledger/{ledgerId}    내역 삭제 + 자산 잔액 복원
 */
@RestController
@RequestMapping("/api/ledger")
public class LedgerController {

    private final LedgerService ledgerService = new LedgerService();

    @GetMapping("/list")
    public ResponseEntity<List<GeneralLedgerDTO>> getLedgerDetail() {
        return ResponseEntity.ok(ledgerService.getAllGeneralLedger());
    }

    @GetMapping("/summary")
    public ResponseEntity<?> getMonthlySummary() {
        return ResponseEntity.ok(ledgerService.getMonthlySummary());
    }

    @GetMapping("/categories")
    public ResponseEntity<?> getCategories() {
        return ResponseEntity.ok(ledgerService.getCategories());
    }

    @PostMapping("/categories")
    public ResponseEntity<?> addCategory(@RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(ledgerService.addCategory((String) body.get("name")));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/liquid-assets")
    public ResponseEntity<?> getLiquidAssets() {
        return ResponseEntity.ok(ledgerService.getLiquidAssets());
    }

    @PostMapping
    public ResponseEntity<?> saveLedger(@RequestBody Map<String, Object> payload) {
        try {
            ledgerService.processGeneralLedger(payload);
            return ResponseEntity.ok(Map.of("message", "내역이 성공적으로 등록되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{ledgerId}")
    public ResponseEntity<?> deleteLedger(@PathVariable int ledgerId) {
        try {
            ledgerService.deleteGeneralLedger(ledgerId);
            return ResponseEntity.ok(Map.of("message", "내역이 삭제되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
