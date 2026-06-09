package com.alm.service;

import com.alm.dto.GeneralLedgerDTO;
import com.alm.repository.LedgerRepository;
import com.alm.util.ParseUtil;
import java.util.List;
import java.util.Map;

/**
 * 가계부 서비스.
 * 내역 저장/삭제 시 연동 자산(ACC/CSH) 잔액을 즉시 반영한다.
 * asset_id = 0 이면 자산 연동 없이 기록만 저장한다.
 */
public class LedgerService {

    private final LedgerRepository ledgerRepository = new LedgerRepository();

    // ── 조회 ─────────────────────────────────────────────────────────

    public List<GeneralLedgerDTO>  getAllGeneralLedger() { return ledgerRepository.findAllGeneralLedger(); }
    public Map<String, Long>       getMonthlySummary()   { return ledgerRepository.getSumByCategory(); }
    public List<Map<String,Object>> getCategories()      { return ledgerRepository.findAllCategories(); }
    public List<Map<String,Object>> getLiquidAssets()    { return ledgerRepository.findLiquidAssets(); }

    public Map<String, Object> addCategory(String categoryName) {
        if (categoryName == null || categoryName.trim().isEmpty())
            throw new IllegalArgumentException("카테고리명을 입력하세요.");
        int id = ledgerRepository.insertCategory(categoryName.trim());
        if (id < 0) throw new RuntimeException("카테고리 추가에 실패했습니다.");
        return Map.of("category_id", id, "category_name", categoryName.trim());
    }

    // ── 저장 ─────────────────────────────────────────────────────────

    /**
     * 가계부 내역 저장 + 연동 자산 잔액 반영.
     * direction "IN" → amount 양수 저장, "OUT" → 음수 저장.
     */
    public void processGeneralLedger(Map<String, Object> payload) throws Exception {
        String direction  = (String) payload.getOrDefault("direction", "OUT");
        long   absAmount  = ParseUtil.parseLong(payload.get("amount"));
        long   signedAmount = "IN".equals(direction) ? absAmount : -absAmount;
        long   assetId    = ParseUtil.parseLong(payload.get("asset_id"));

        int ledgerId = ledgerRepository.insertLedgerMaster("GEN");
        if (ledgerId == -1) throw new Exception("가계부 마스터 생성에 실패했습니다.");

        GeneralLedgerDTO dto = new GeneralLedgerDTO();
        dto.setLedger_id(ledgerId);
        dto.setType_code("GEN");
        dto.setAsset_id(assetId);
        dto.setCategory_id(ParseUtil.parseInt(payload.get("category_id"), 1));
        dto.setAmount(signedAmount);
        dto.setTransaction_date((String) payload.getOrDefault("transaction_date", ""));

        if (!ledgerRepository.insertGeneralLedger(dto))
            throw new Exception("가계부 내역 저장에 실패했습니다.");

        if (assetId > 0) ledgerRepository.adjustAssetBalance(assetId, signedAmount);
    }

    // ── 삭제 ─────────────────────────────────────────────────────────

    /**
     * 가계부 내역 삭제 + 연동 자산 잔액 복원.
     * 삭제 전 기존 amount 를 조회해 부호 반전으로 잔액을 되돌린다.
     */
    public void deleteGeneralLedger(int ledgerId) throws Exception {
        GeneralLedgerDTO existing = ledgerRepository.findGeneralLedgerById(ledgerId);
        if (existing == null) throw new Exception("해당 내역을 찾을 수 없습니다.");

        long assetId    = existing.getAsset_id();
        long savedAmount = existing.getAmount();

        if (!ledgerRepository.deleteLedgerMaster(ledgerId))
            throw new Exception("삭제에 실패했습니다.");

        if (assetId > 0) ledgerRepository.adjustAssetBalance(assetId, -savedAmount);
    }

}
