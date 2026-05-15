package com.alm.dto;

/**
 * [DTO 계층 - 고정지출 영수증]
 * 고정지출 규칙이 실행되어 생성된 거래 내역(영수증)을 담는 DTO.
 * fixed_expense_receipt 테이블과 1:1로 매핑됨.
 *
 * [Class Table Inheritance 패턴 참여]
 *   - LedgerDTO를 상속: ledger_master 테이블의 ledger_id를 공유.
 *   - fixed_expense_receipt 테이블: ledger_id(PK/FK) + 이 클래스의 추가 필드.
 *   - 규칙(FixedExpenseRuleDTO)과 달리 실제 거래 기록이므로 ledger_id가 필요.
 *
 * [DB 테이블: fixed_expense_receipt]
 *   - ledger_id         INT PK/FK (→ ledger_master)
 *   - asset_id          BIGINT FK (→ asset_master)
 *   - category_id       INT FK (→ ledger_category)
 *   - rule_id           INT FK (→ fixed_expense_rule)
 *   - amount            BIGINT: 지출 금액 (항상 음수)
 *   - transaction_date  DATE
 */
public class FixedExpenseReceiptDTO extends LedgerDTO {

    // 이 영수증을 생성한 고정지출 규칙의 ID.
    // FK: fixed_expense_rule.rule_id를 참조.
    private int rule_id;

    // 연동된 자산의 식별자. FK: asset_master.asset_id.
    // long 타입: asset_master.asset_id가 BIGINT이므로 long으로 수용.
    private long asset_id;

    // 카테고리 식별자. FK: ledger_category.category_id.
    private int category_id;

    // 실제 집행된 금액. 지출이므로 항상 음수로 저장됨.
    // 예: 월세 50만원 → amount = -500000
    private long amount;

    // 실제 거래 발생일. "YYYY-MM-DD" 형식 문자열.
    private String transaction_date;

    // ── JOIN 표시용 필드 (DB 컬럼에 저장되지 않음) ──────────────
    // LEFT JOIN ledger_category 결과
    private String category_name;
    // COALESCE(은행명+계좌 뒤4자리, 현금명) 결과
    private String asset_name;
    // LEFT JOIN fixed_expense_rule 결과: rule.name
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

    public String getCategory_name()                    { return category_name; }
    public void setCategory_name(String category_name)  { this.category_name = category_name; }

    public String getAsset_name()                  { return asset_name; }
    public void setAsset_name(String asset_name)   { this.asset_name = asset_name; }

    public String getRule_name()                 { return rule_name; }
    public void setRule_name(String rule_name)   { this.rule_name = rule_name; }
}
