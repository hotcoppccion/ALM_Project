package com.alm.service;

import com.alm.dto.GeneralLedgerDTO;
import com.alm.repository.LedgerRepository;
import com.alm.util.ParseUtil;
import java.util.List;
import java.util.Map;

/**
 * 가계부 도메인 비즈니스 로직.
 *
 * [핵심 비즈니스 규칙]
 *   1. 내역 저장 시 연동 자산(ACC / CSH)의 잔액을 즉시 반영한다.
 *   2. 내역 삭제 시 저장된 금액의 부호를 반전해 잔액을 원상 복원한다.
 *   3. asset_id = 0 이면 자산 연동 없이 기록만 저장한다 (연동 선택 사항).
 *
 * [트랜잭션 부재]
 *   순수 JDBC 환경으로 @Transactional 미사용.
 *   내역 저장 성공 후 잔액 조정 실패 시 불일치 가능성이 있다. (향후 Spring TX 로 해결 예정)
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
     *
     * [amount 부호 규칙]
     *   프론트엔드는 금액을 항상 양수로 전송하고, direction("IN"/"OUT") 으로 방향을 구분한다.
     *   Service 에서 부호를 적용해 DB 에 저장한다: 수입 → +, 지출 → -.
     *   잔액 조정도 동일한 부호 금액으로 전달하면 수입은 잔액 증가, 지출은 잔액 감소가 된다.
     *
     * @param payload { asset_id, category_id, amount(양수), direction, transaction_date }
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
     *
     * [복원 로직]
     *   저장 시 지출(-10000)로 기록된 내역을 삭제하면 adjustAssetBalance(assetId, +10000) 으로 복원.
     *   삭제 전에 기존 amount 를 반드시 조회해야 한다 (삭제 후에는 조회 불가).
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
