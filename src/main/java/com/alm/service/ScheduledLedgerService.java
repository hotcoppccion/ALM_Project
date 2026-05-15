package com.alm.service;

import com.alm.dto.FixedExpenseReceiptDTO;
import com.alm.dto.FixedExpenseRuleDTO;
import com.alm.dto.RegularIncomeReceiptDTO;
import com.alm.dto.RegularIncomeRuleDTO;
import com.alm.dto.VariableExpenseReceiptDTO;
import com.alm.dto.VariableExpenseRuleDTO;
import com.alm.repository.LedgerRepository;
import com.alm.repository.ScheduledLedgerRepository;
import java.util.List;
import java.util.Map;

/**
 * [Service 계층 - Business Logic]
 * 고정지출 / 정기수입 / 변동지출 3개 도메인의 비즈니스 로직을 수행하는 클래스.
 *
 * [계층 구조]
 *   LedgerController → ScheduledLedgerService → ScheduledLedgerRepository → DB
 *                                              → LedgerRepository (공통 메서드 재사용)
 *
 * [주요 비즈니스 규칙]
 *   고정지출/정기수입 규칙 실행:
 *     1. ledger_master INSERT (insertLedgerMaster) → ledger_id 발급
 *     2. 전용 영수증 테이블 INSERT
 *     3. 연동 자산 잔액 즉시 반영 (adjustAssetBalance)
 *
 *   영수증 삭제:
 *     1. 삭제 전 asset_id + amount 조회 (영수증 단건 조회)
 *     2. ledger_master DELETE (CASCADE로 영수증 자동 삭제)
 *     3. 자산 잔액 역방향 복원
 *
 *   변동지출 특이사항:
 *     - 규칙 발동 시 PENDING 상태 영수증만 생성 (amount=0, 자산 잔액 변동 없음)
 *     - 사용자 확정 시 amount UPDATE + 자산 잔액 차감
 *     - PENDING 영수증 삭제: 자산 잔액 복원 불필요 (amount=0이었으므로)
 *     - CONFIRMED 영수증 삭제: 자산 잔액 복원 필요
 */
public class ScheduledLedgerService {

    // Repository 인스턴스 (final: 한 번 초기화 후 재할당 불가)
    private final ScheduledLedgerRepository repo       = new ScheduledLedgerRepository();
    private final LedgerRepository          ledgerRepo = new LedgerRepository();

    // ═══════════════════════════════════════════════════════════
    //  고정지출 규칙 (fixed_expense_rule)
    // ═══════════════════════════════════════════════════════════

    /**
     * 고정지출 규칙 전체 목록 반환.
     */
    public List<FixedExpenseRuleDTO> getFixedRules() {
        return repo.findAllFixedRules();
    }

    /**
     * 고정지출 규칙 등록.
     * payload에서 name, category_id, amount, base_date, p_value, p_unit을 추출하여 저장.
     *
     * @param payload 프론트엔드 JSON → Map
     * @throws Exception 필수 값 누락 또는 저장 실패 시
     */
    public void saveFixedRule(Map<String, Object> payload) throws Exception {
        FixedExpenseRuleDTO dto = new FixedExpenseRuleDTO();
        dto.setName((String) payload.getOrDefault("name", "미지정"));
        dto.setCategory_id(parseIntSafe(payload.get("category_id"), 0));
        dto.setAmount(parseLongSafe(payload.get("amount")));
        dto.setBase_date((String) payload.getOrDefault("base_date", ""));
        dto.setP_value(parseIntSafe(payload.get("p_value"), 1));
        dto.setP_unit((String) payload.getOrDefault("p_unit", "MONTH"));

        if (dto.getAmount() <= 0)      throw new Exception("금액을 올바르게 입력하세요.");
        if (dto.getBase_date().isEmpty()) throw new Exception("기준일을 선택하세요.");

        int id = repo.insertFixedRule(dto);
        if (id == -1) throw new Exception("고정지출 규칙 등록에 실패했습니다.");
    }

    /**
     * 고정지출 규칙 삭제.
     *
     * @param ruleId 삭제할 규칙 ID
     * @throws Exception 삭제 실패 시
     */
    public void deleteFixedRule(int ruleId) throws Exception {
        boolean ok = repo.deleteFixedRule(ruleId);
        if (!ok) throw new Exception("고정지출 규칙 삭제에 실패했습니다.");
    }

    /**
     * 고정지출 규칙 실행: 규칙 조회 → 마스터 생성 → 영수증 삽입 → 자산 잔액 차감.
     *
     * @param ruleId  실행할 규칙 ID
     * @param payload {asset_id, transaction_date} (규칙 실행 시 사용자가 자산과 날짜 선택)
     * @throws Exception 규칙 없음 또는 처리 실패 시
     */
    public void executeFixedRule(int ruleId, Map<String, Object> payload) throws Exception {
        // 1. 규칙 조회 (amount, category_id 가져오기)
        FixedExpenseRuleDTO rule = repo.findFixedRuleById(ruleId);
        if (rule == null) throw new Exception("고정지출 규칙을 찾을 수 없습니다.");

        long assetId = parseLongSafe(payload.get("asset_id"));
        String txDate = (String) payload.getOrDefault("transaction_date", "");
        if (txDate.isEmpty()) throw new Exception("거래일을 선택하세요.");

        // 2. ledger_master에 부모 레코드 생성 (타입코드: FIX_R)
        int ledgerId = ledgerRepo.insertLedgerMaster("FIX_R");
        if (ledgerId == -1) throw new Exception("가계부 마스터 생성에 실패했습니다.");

        // 3. 영수증 DTO 조립 (amount는 지출이므로 음수)
        FixedExpenseReceiptDTO receipt = new FixedExpenseReceiptDTO();
        receipt.setLedger_id(ledgerId);
        receipt.setRule_id(ruleId);
        receipt.setAsset_id(assetId);
        receipt.setCategory_id(rule.getCategory_id());
        receipt.setAmount(-rule.getAmount());   // 부호 반전: 지출 → 음수
        receipt.setTransaction_date(txDate);

        // 4. 영수증 삽입
        boolean saved = repo.insertFixedReceipt(receipt);
        if (!saved) throw new Exception("고정지출 영수증 저장에 실패했습니다.");

        // 5. 자산 잔액 차감 (delta = 음수 = 잔액 감소)
        if (assetId > 0) {
            ledgerRepo.adjustAssetBalance(assetId, -rule.getAmount());
        }
    }

    // ─────────────────────────────────────────────────────────
    //  고정지출 영수증
    // ─────────────────────────────────────────────────────────

    /**
     * 고정지출 영수증 전체 목록 반환.
     */
    public List<FixedExpenseReceiptDTO> getFixedReceipts() {
        return repo.findAllFixedReceipts();
    }

    /**
     * 고정지출 영수증 삭제 + 자산 잔액 복원.
     *
     * @param ledgerId 삭제할 영수증 ID
     * @throws Exception 없는 내역 또는 삭제 실패 시
     */
    public void deleteFixedReceipt(int ledgerId) throws Exception {
        // 1. 삭제 전 데이터 조회 (복원용 asset_id, amount)
        FixedExpenseReceiptDTO existing = repo.findFixedReceiptById(ledgerId);
        if (existing == null) throw new Exception("해당 고정지출 내역을 찾을 수 없습니다.");

        long assetId     = existing.getAsset_id();
        long savedAmount = existing.getAmount();   // 음수 (지출로 저장됨)

        // 2. ledger_master 삭제 → CASCADE로 fixed_expense_receipt 자동 삭제
        boolean deleted = ledgerRepo.deleteLedgerMaster(ledgerId);
        if (!deleted) throw new Exception("고정지출 내역 삭제에 실패했습니다.");

        // 3. 잔액 복원: 지출(음수)의 반대 방향(양수)으로 복원
        //    savedAmount = -500000 → -savedAmount = +500000 → 잔액 증가
        if (assetId > 0) {
            ledgerRepo.adjustAssetBalance(assetId, -savedAmount);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  정기수입 규칙 (regular_income_rule)
    // ═══════════════════════════════════════════════════════════

    /**
     * 정기수입 규칙 전체 목록 반환.
     */
    public List<RegularIncomeRuleDTO> getIncomeRules() {
        return repo.findAllIncomeRules();
    }

    /**
     * 정기수입 규칙 등록.
     *
     * @param payload 프론트엔드 JSON → Map
     * @throws Exception 필수 값 누락 또는 저장 실패 시
     */
    public void saveIncomeRule(Map<String, Object> payload) throws Exception {
        RegularIncomeRuleDTO dto = new RegularIncomeRuleDTO();
        dto.setName((String) payload.getOrDefault("name", "미지정"));
        dto.setCategory_id(parseIntSafe(payload.get("category_id"), 0));
        dto.setAmount(parseLongSafe(payload.get("amount")));
        dto.setBase_date((String) payload.getOrDefault("base_date", ""));
        dto.setP_value(parseIntSafe(payload.get("p_value"), 1));
        dto.setP_unit((String) payload.getOrDefault("p_unit", "MONTH"));

        if (dto.getAmount() <= 0)      throw new Exception("금액을 올바르게 입력하세요.");
        if (dto.getBase_date().isEmpty()) throw new Exception("기준일을 선택하세요.");

        int id = repo.insertIncomeRule(dto);
        if (id == -1) throw new Exception("정기수입 규칙 등록에 실패했습니다.");
    }

    /**
     * 정기수입 규칙 삭제.
     *
     * @param ruleId 삭제할 규칙 ID
     * @throws Exception 삭제 실패 시
     */
    public void deleteIncomeRule(int ruleId) throws Exception {
        boolean ok = repo.deleteIncomeRule(ruleId);
        if (!ok) throw new Exception("정기수입 규칙 삭제에 실패했습니다.");
    }

    /**
     * 정기수입 규칙 실행: 규칙 조회 → 마스터 생성 → 영수증 삽입 → 자산 잔액 증가.
     *
     * @param ruleId  실행할 규칙 ID
     * @param payload {asset_id, transaction_date}
     * @throws Exception 규칙 없음 또는 처리 실패 시
     */
    public void executeIncomeRule(int ruleId, Map<String, Object> payload) throws Exception {
        // 1. 규칙 조회
        RegularIncomeRuleDTO rule = repo.findIncomeRuleById(ruleId);
        if (rule == null) throw new Exception("정기수입 규칙을 찾을 수 없습니다.");

        long assetId = parseLongSafe(payload.get("asset_id"));
        String txDate = (String) payload.getOrDefault("transaction_date", "");
        if (txDate.isEmpty()) throw new Exception("거래일을 선택하세요.");

        // 2. ledger_master에 부모 레코드 생성 (타입코드: REG_R)
        int ledgerId = ledgerRepo.insertLedgerMaster("REG_R");
        if (ledgerId == -1) throw new Exception("가계부 마스터 생성에 실패했습니다.");

        // 3. 영수증 DTO 조립 (amount는 수입이므로 양수)
        RegularIncomeReceiptDTO receipt = new RegularIncomeReceiptDTO();
        receipt.setLedger_id(ledgerId);
        receipt.setRule_id(ruleId);
        receipt.setAsset_id(assetId);
        receipt.setCategory_id(rule.getCategory_id());
        receipt.setAmount(rule.getAmount());   // 수입 → 양수 그대로
        receipt.setTransaction_date(txDate);

        // 4. 영수증 삽입
        boolean saved = repo.insertIncomeReceipt(receipt);
        if (!saved) throw new Exception("정기수입 영수증 저장에 실패했습니다.");

        // 5. 자산 잔액 증가 (delta = 양수 = 잔액 증가)
        if (assetId > 0) {
            ledgerRepo.adjustAssetBalance(assetId, rule.getAmount());
        }
    }

    // ─────────────────────────────────────────────────────────
    //  정기수입 영수증
    // ─────────────────────────────────────────────────────────

    /**
     * 정기수입 영수증 전체 목록 반환.
     */
    public List<RegularIncomeReceiptDTO> getIncomeReceipts() {
        return repo.findAllIncomeReceipts();
    }

    /**
     * 정기수입 영수증 삭제 + 자산 잔액 복원.
     *
     * @param ledgerId 삭제할 영수증 ID
     * @throws Exception 없는 내역 또는 삭제 실패 시
     */
    public void deleteIncomeReceipt(int ledgerId) throws Exception {
        // 1. 삭제 전 데이터 조회 (복원용 asset_id, amount)
        RegularIncomeReceiptDTO existing = repo.findIncomeReceiptById(ledgerId);
        if (existing == null) throw new Exception("해당 정기수입 내역을 찾을 수 없습니다.");

        long assetId     = existing.getAsset_id();
        long savedAmount = existing.getAmount();   // 양수 (수입으로 저장됨)

        // 2. ledger_master 삭제 → CASCADE로 regular_income_receipt 자동 삭제
        boolean deleted = ledgerRepo.deleteLedgerMaster(ledgerId);
        if (!deleted) throw new Exception("정기수입 내역 삭제에 실패했습니다.");

        // 3. 잔액 복원: 수입(양수)의 반대 방향(음수)으로 복원
        //    savedAmount = +500000 → -savedAmount = -500000 → 잔액 감소
        if (assetId > 0) {
            ledgerRepo.adjustAssetBalance(assetId, -savedAmount);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  변동지출 규칙 (variable_expense_rule)
    // ═══════════════════════════════════════════════════════════

    /**
     * 변동지출 규칙 전체 목록 반환.
     */
    public List<VariableExpenseRuleDTO> getVariableRules() {
        return repo.findAllVariableRules();
    }

    /**
     * 변동지출 규칙 등록 (amount 없음).
     *
     * @param payload {name, category_id, base_date, p_value, p_unit}
     * @throws Exception 필수 값 누락 또는 저장 실패 시
     */
    public void saveVariableRule(Map<String, Object> payload) throws Exception {
        VariableExpenseRuleDTO dto = new VariableExpenseRuleDTO();
        dto.setName((String) payload.getOrDefault("name", "미지정"));
        dto.setCategory_id(parseIntSafe(payload.get("category_id"), 0));
        dto.setBase_date((String) payload.getOrDefault("base_date", ""));
        dto.setP_value(parseIntSafe(payload.get("p_value"), 1));
        dto.setP_unit((String) payload.getOrDefault("p_unit", "MONTH"));

        if (dto.getBase_date().isEmpty()) throw new Exception("기준일을 선택하세요.");

        int id = repo.insertVariableRule(dto);
        if (id == -1) throw new Exception("변동지출 규칙 등록에 실패했습니다.");
    }

    /**
     * 변동지출 규칙 삭제.
     *
     * @param ruleId 삭제할 규칙 ID
     * @throws Exception 삭제 실패 시
     */
    public void deleteVariableRule(int ruleId) throws Exception {
        boolean ok = repo.deleteVariableRule(ruleId);
        if (!ok) throw new Exception("변동지출 규칙 삭제에 실패했습니다.");
    }

    /**
     * 변동지출 규칙 발동: PENDING 상태 영수증 생성 (금액 미확정, 자산 잔액 변동 없음).
     *
     * @param ruleId  발동할 규칙 ID
     * @param payload {asset_id, transaction_date}
     * @throws Exception 규칙 없음 또는 처리 실패 시
     */
    public void triggerVariableRule(int ruleId, Map<String, Object> payload) throws Exception {
        // 1. 규칙 조회 (category_id 가져오기)
        VariableExpenseRuleDTO rule = repo.findVariableRuleById(ruleId);
        if (rule == null) throw new Exception("변동지출 규칙을 찾을 수 없습니다.");

        long assetId = parseLongSafe(payload.get("asset_id"));
        String txDate = (String) payload.getOrDefault("transaction_date", "");
        if (txDate.isEmpty()) throw new Exception("거래일을 선택하세요.");

        // 2. ledger_master에 부모 레코드 생성 (타입코드: VAR_R)
        int ledgerId = ledgerRepo.insertLedgerMaster("VAR_R");
        if (ledgerId == -1) throw new Exception("가계부 마스터 생성에 실패했습니다.");

        // 3. 영수증 DTO 조립 (amount=0, status='PENDING')
        //    VariableExpenseReceiptDTO.insertVariableReceipt()에서 amount=0, status='PENDING' 고정
        VariableExpenseReceiptDTO receipt = new VariableExpenseReceiptDTO();
        receipt.setLedger_id(ledgerId);
        receipt.setRule_id(ruleId);
        receipt.setAsset_id(assetId);
        receipt.setCategory_id(rule.getCategory_id());
        receipt.setTransaction_date(txDate);
        // amount와 status는 Repository SQL에서 0/'PENDING'으로 고정 삽입

        // 4. 영수증 삽입 (PENDING 상태)
        boolean saved = repo.insertVariableReceipt(receipt);
        if (!saved) throw new Exception("변동지출 영수증(PENDING) 저장에 실패했습니다.");

        // 5. PENDING 상태이므로 자산 잔액 변동 없음
    }

    // ─────────────────────────────────────────────────────────
    //  변동지출 영수증
    // ─────────────────────────────────────────────────────────

    /**
     * 변동지출 영수증 전체 목록 반환.
     */
    public List<VariableExpenseReceiptDTO> getVariableReceipts() {
        return repo.findAllVariableReceipts();
    }

    /**
     * 변동지출 영수증 확정: PENDING → CONFIRMED 상태 변경 + 실제 금액 저장 + 자산 잔액 차감.
     *
     * @param ledgerId 확정할 영수증 ID
     * @param amount   사용자가 입력한 실제 지출 금액 (양수; 내부에서 음수로 변환)
     * @throws Exception 없는 내역, 이미 확정됨, 처리 실패 시
     */
    public void confirmVariableReceipt(int ledgerId, long amount) throws Exception {
        // 1. 영수증 단건 조회 (status 확인 + asset_id 획득)
        VariableExpenseReceiptDTO existing = repo.findVariableReceiptById(ledgerId);
        if (existing == null) throw new Exception("해당 변동지출 내역을 찾을 수 없습니다.");
        if (!"PENDING".equals(existing.getStatus())) {
            throw new Exception("이미 확정된 내역입니다.");
        }

        long assetId      = existing.getAsset_id();
        long signedAmount = -Math.abs(amount);   // 지출이므로 반드시 음수로 저장

        // 2. status='CONFIRMED', amount=음수 로 갱신
        boolean updated = repo.confirmVariableReceipt(ledgerId, signedAmount);
        if (!updated) throw new Exception("변동지출 확정 처리에 실패했습니다.");

        // 3. 자산 잔액 차감 (delta = 음수 = 잔액 감소)
        if (assetId > 0) {
            ledgerRepo.adjustAssetBalance(assetId, signedAmount);
        }
    }

    /**
     * 변동지출 영수증 삭제 + 자산 잔액 복원 (CONFIRMED인 경우에만).
     *
     * @param ledgerId 삭제할 영수증 ID
     * @throws Exception 없는 내역 또는 삭제 실패 시
     */
    public void deleteVariableReceipt(int ledgerId) throws Exception {
        // 1. 삭제 전 데이터 조회
        VariableExpenseReceiptDTO existing = repo.findVariableReceiptById(ledgerId);
        if (existing == null) throw new Exception("해당 변동지출 내역을 찾을 수 없습니다.");

        long assetId     = existing.getAsset_id();
        long savedAmount = existing.getAmount();   // PENDING이면 0, CONFIRMED이면 음수
        String status    = existing.getStatus();

        // 2. ledger_master 삭제 → CASCADE로 variable_expense_receipt 자동 삭제
        boolean deleted = ledgerRepo.deleteLedgerMaster(ledgerId);
        if (!deleted) throw new Exception("변동지출 내역 삭제에 실패했습니다.");

        // 3. CONFIRMED 상태였다면 잔액 복원 (PENDING은 amount=0이므로 복원 불필요)
        if ("CONFIRMED".equals(status) && assetId > 0 && savedAmount != 0) {
            // savedAmount = -100000 → -savedAmount = +100000 → 잔액 복원
            ledgerRepo.adjustAssetBalance(assetId, -savedAmount);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  유틸리티 메서드 (파싱 방어 로직)
    // ═══════════════════════════════════════════════════════════

    /**
     * Object → long 안전 변환.
     * JSON으로 전달된 숫자가 Integer, Long, String 등 다양한 타입으로 역직렬화될 수 있으므로
     * toString() 후 Long.parseLong()으로 통일. 실패 시 0L 반환.
     */
    private long parseLongSafe(Object obj) {
        if (obj == null) return 0L;
        String str = obj.toString().trim();
        if (str.isEmpty() || "null".equals(str)) return 0L;
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * Object → int 안전 변환. 실패 시 defaultVal 반환.
     */
    private int parseIntSafe(Object obj, int defaultVal) {
        if (obj == null) return defaultVal;
        String str = obj.toString().trim();
        if (str.isEmpty() || "null".equals(str)) return defaultVal;
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }
}
