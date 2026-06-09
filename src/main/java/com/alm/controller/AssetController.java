package com.alm.controller;

import com.alm.service.AssetService;
import com.alm.dto.AssetDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/**
 * 자산 도메인 REST API.
 *
 * [API 목록]
 *   GET    /api/asset/list              전체 자산 목록 (4개 타입 합산)
 *   GET    /api/asset/total             전체 자산 합계 금액
 *   GET    /api/asset/{assetId}         단건 조회 (수정 모달 pre-fill)
 *   GET    /api/asset/{assetId}/interest   예상 연간 이자 수익금 조회
 *   POST   /api/asset/{assetId}/interest   이자 지급 (잔액 반영 + 가계부 수입 기록)
 *   GET    /api/asset/banks             은행 드롭다운
 *   GET    /api/asset/account-types     계좌 종류 드롭다운
 *   POST   /api/asset/banks             은행 추가
 *   POST   /api/asset/account-types     계좌 종류 추가
 *   POST   /api/asset/{typeCode}        자산 등록
 *   PUT    /api/asset/{assetId}         자산 수정
 *   DELETE /api/asset/{assetId}         자산 삭제 (확인 문자열 + 참조 검증)
 */
@RestController
@RequestMapping("/api/asset")
public class AssetController {

    private final AssetService assetService = new AssetService();

    // ── 조회 ─────────────────────────────────────────────────────────

    @GetMapping("/list")
    public ResponseEntity<List<AssetDTO>> getAllAssets() {
        return ResponseEntity.ok(assetService.getAllAssets());
    }

    @GetMapping("")
    public ResponseEntity<List<AssetDTO>> getAssetList() {
        return ResponseEntity.ok(assetService.getAllAssets());
    }

    @GetMapping("/total")
    public ResponseEntity<?> getTotalAsset() {
        return ResponseEntity.ok(Map.of("totalAmount", assetService.calculateTotalAsset()));
    }

    @GetMapping("/{assetId}")
    public ResponseEntity<?> getAssetById(@PathVariable long assetId,
                                          @RequestParam String typeCode) {
        AssetDTO dto = assetService.getAssetById(assetId, typeCode);
        if (dto == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{assetId}/interest")
    public ResponseEntity<?> getAccountInterest(@PathVariable long assetId) {
        double interest = assetService.getAccountInterest(assetId);
        return ResponseEntity.ok(Map.of("asset_id", assetId, "annual_interest", interest));
    }

    @PostMapping("/{assetId}/interest")
    public ResponseEntity<?> applyAccountInterest(@PathVariable long assetId) {
        try {
            assetService.applyAccountInterest(assetId);
            return ResponseEntity.ok(Map.of("message", "이자가 지급되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/banks")
    public ResponseEntity<?> getBanks() {
        return ResponseEntity.ok(assetService.getBanks());
    }

    @GetMapping("/account-types")
    public ResponseEntity<?> getAccountTypes() {
        return ResponseEntity.ok(assetService.getAccountTypes());
    }

    @PostMapping("/banks")
    public ResponseEntity<?> addBank(@RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(assetService.addBank((String) body.get("name")));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/account-types")
    public ResponseEntity<?> addAccountType(@RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(assetService.addAccountType((String) body.get("name")));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ── 저장 ─────────────────────────────────────────────────────────

    @PostMapping("/{typeCode}")
    public ResponseEntity<?> registerAsset(@PathVariable String typeCode,
                                           @RequestBody Map<String, Object> payload) {
        try {
            assetService.saveAssetDetails(typeCode.toUpperCase(), payload);
            return ResponseEntity.ok(Map.of("message", "자산이 성공적으로 등록되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ── 수정 ─────────────────────────────────────────────────────────

    @PutMapping("/{assetId}")
    public ResponseEntity<?> updateAsset(@PathVariable long assetId,
                                         @RequestBody Map<String, Object> payload) {
        try {
            String typeCode = (String) payload.get("type_code");
            if (typeCode == null) return ResponseEntity.badRequest().body(Map.of("message", "type_code 누락"));
            assetService.updateAssetDetails(assetId, typeCode.toUpperCase(), payload);
            return ResponseEntity.ok(Map.of("message", "자산이 수정되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ── 삭제 ─────────────────────────────────────────────────────────

    @DeleteMapping("/{assetId}")
    public ResponseEntity<?> deleteAsset(@PathVariable long assetId,
                                         @RequestParam String confirmString) {
        try {
            assetService.requestDeleteAsset(assetId, confirmString);
            return ResponseEntity.ok(Map.of("message", "자산이 삭제되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
