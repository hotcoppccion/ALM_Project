package com.alm.dto;

/**
 * [DTO 계층 - 변동지출 영수증]
 * 변동지출 규칙이 발동된 후 생성된 거래 내역 DTO.
 * variable_expense_receipt 테이블과 1:1로 매핑됨.
 *
 * [상속 구조]
 *   LedgerDTO (abstract)
 *     └── VariableExpenseReceiptDTO  ← 이 클래스
 *
 * [다른 영수증 DTO와의 차이점]
 *   - status 필드 존재: PENDING(미확정) / CONFIRMED(확정) 두 상태 관리.
 *   - PENDING 상태에서는 amount = 0 (아직 금액 미입력).
 *   - 사용자가 confirmVariableReceipt() 호출 시 amount 갱신 + 자산 잔액 차감.
 *
 * [DB 테이블: variable_expense_receipt]
 *   - ledger_id         INT PK/FK (→ ledger_master)
 *   - asset_id          BIGINT FK (→ asset_master)
 *   - category_id       INT FK (→ ledger_category)
 *   - rule_id           INT FK (→ variable_expense_rule)
 *   - amount            BIGINT DEFAULT 0: PENDING 시 0, CONFIRMED 시 실제 금액(음수)
 *   - transaction_date  DATE
 *   - status            VARCHAR(20) DEFAULT 'PENDING'
 */
public class VariableExpenseReceiptDTO extends LedgerDTO {

    // 이 영수증을 생성한 변동지출 규칙의 ID. FK 참조.
    private int rule_id;

    // 연동된 자산의 식별자. FK: asset_master.asset_id.
    // long 타입: asset_master.asset_id가 BIGINT이므로 long으로 수용.
    private long asset_id;

    // 카테고리 식별자. FK: ledger_category.category_id.
    private int category_id;

    // 사용자가 나중에 입력한 실제 지출 금액. 음수로 저장됨.
    // status가 "PENDING"일 때는 0. "CONFIRMED" 시 실제 금액으로 갱신.
    private long amount;

    // 거래 발생일. "YYYY-MM-DD" 형식.
    private String transaction_date;

    // 처리 상태.
    // "PENDING"  : 규칙이 발동했으나 금액 미입력 상태 (사용자 액션 필요)
    // "CONFIRMED": 사용자가 금액을 입력하여 확정 완료
    private String status;

    // ── JOIN 표시용 필드 (DB 컬럼에 저장되지 않음) ──────────────
    // LEFT JOIN ledger_category 결과
    private String category_name;
    // COALESCE(은행명+계좌 뒤4자리, 현금명) 결과
    private String asset_name;
    // LEFT JOIN variable_expense_rule 결과: rule.name
    private String rule_name;

    // ── Getter / Setter ──────────────────────────────────────────────
    // 부모(LedgerDTO)의 getLedger_id(), getType_code() 등은 상속으로 이미 보유.

    public int getRule_id()               { return rule_id; }
    public void setRule_id(int rule_id)   { this.rule_id = rule_id; }

    public long getAsset_id()              { return asset_id; }
    public void setAsset_id(long asset_id) { this.asset_id = asset_id; }

    public int getCategory_id()                   { return category_id; }
    public void setCategory_id(int category_id)   { this.category_id = category_id; }

    public long getAmount()               { return amount; }
    public void setAmount(long amount)    { this.amount = amount; }

    public String getTransaction_date()                       { return transaction_date; }
    public void setTransaction_date(String transaction_date)  { this.transaction_date = transaction_date; }

    public String getStatus()               { return status; }
    public void setStatus(String status)    { this.status = status; }

    public String getCategory_name()                    { return category_name; }
    public void setCategory_name(String category_name)  { this.category_name = category_name; }

    public String getAsset_name()                  { return asset_name; }
    public void setAsset_name(String asset_name)   { this.asset_name = asset_name; }

    public String getRule_name()                 { return rule_name; }
    public void setRule_name(String rule_name)   { this.rule_name = rule_name; }
}
