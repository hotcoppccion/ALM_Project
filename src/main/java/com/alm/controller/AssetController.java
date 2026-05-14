package com.alm.controller;

import com.alm.service.AssetService;
import com.alm.repository.AssetRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/asset")
public class AssetController {

    private final AssetService assetService = new AssetService();
    private final AssetRepository assetRepository = new AssetRepository();

    /**
     * 1. 자산 신규 등록 (팝업 저장 시 호출)
     * AssetRepository에 있는 insertAssetMaster만 호출하여 마스터 ID를 발급받습니다.
     */
    @PostMapping("/{typeCode}")
    public ResponseEntity<?> insertAsset(@PathVariable String typeCode) {
        // 기획서의 type_code (예: ACC, REA, PHY, CSH)
        long newId = assetRepository.insertAssetMaster(typeCode.toUpperCase());

        if (newId != -1) {
            return ResponseEntity.ok(Map.of("message", "등록 성공 (마스터 ID: " + newId + ")"));
        }
        return ResponseEntity.internalServerError().body(Map.of("message", "자산 등록 실패"));
    }

    /**
     * 2. 자산 삭제 (우클릭 삭제 시 호출)
     * AssetService에 있는 requestDeleteAsset(검증 로직)만 호출합니다.
     */
    @DeleteMapping("/{assetId}")
    public ResponseEntity<?> deleteAsset(@PathVariable long assetId) {
        try {
            assetService.requestDeleteAsset(assetId);
            return ResponseEntity.ok(Map.of("message", "자산이 삭제되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 3. 총자산 합계 조회
     * AssetService에 이미 만들어둔 calculateTotalAsset을 그대로 사용합니다.
     */
    @GetMapping("/total")
    public ResponseEntity<?> getTotalAsset() {
        long total = assetService.calculateTotalAsset();
        return ResponseEntity.ok(Map.of("totalAmount", total));
    }
}