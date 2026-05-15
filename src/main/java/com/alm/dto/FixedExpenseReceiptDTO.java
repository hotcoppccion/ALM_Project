package com.alm.dto;

/**
 * [DTO 계층 - 고정지출 영수증]
 * 고정지출 규칙이 실행되어 생성된 거래 내역(영수증)을 담는 DTO.
 * fixed_expense_receipt 테이블과 1:1로 매핑됨. 기획서 5.3절에 정의된 클래스.
 *
 * [Class Table Inheritance 패턴 참여]
 *   - LedgerDTO를 상속: ledger_master 테이블의 ledger_id를 공유.
 *   - fixed_expense_receipt 테이블: ledger_id(PK/FK) + 이 클래스의 추가 필드.
 *   - 규칙(FixedExpenseRuleDTO)과 달리 실제 거래 기록이므로 ledger_id가 필요.
 *
 * [Java 문법 개념] extends (상속):
 *   - "extends LedgerDTO": FixedExpenseReceiptDTO는 LedgerDTO의 하위 클래스.
 *   - 상속으로 getLedger_id(), getType_code() 등 부모 클래스의 메서드를 자동으로 보유.
 *   - "is-a" 관계: FixedExpenseReceiptDTO IS-A LedgerDTO (고정지출 영수증은 가계부 내역이다).
 *
 * [DB 테이블: fixed_expense_receipt]
 *   - ledger_id         INT PK/FK (→ ledger_master): 부모 레코드 참조
 *   - rule_id           INT FK (→ fixed_expense_rule): 어느 규칙에서 생성됐는지
 *   - amount            BIGINT: 실제 집행 금액 (부호 포함: 지출이므로 항상 음수)
 *   - transaction_date  DATE: 실제 거래 발생일
 */
public class FixedExpenseReceiptDTO extends LedgerDTO {

    // 이 영수증을 생성한 고정지출 규칙의 ID.
    // FK: fixed_expense_rule.rule_id를 참조. 어느 설정에서 발생한 지출인지 역추적 가능.
    private int rule_id;

    // 실제 집행된 금액. 지출이므로 항상 음수로 저장됨.
    // 예: 월세 50만원 → amount = -500000
    private long amount;

    // 실제 거래 발생일. "YYYY-MM-DD" 형식 문자열.
    // FixedExpenseRuleDTO.base_date를 기준으로 LedgerService.calculateNextDate()가 계산.
    private String transaction_date;

    // ── Getter / Setter ──────────────────────────────────────────────
    // [Java 문법 개념] 부모 클래스 메서드 상속:
    //   - getLedger_id(), setLedger_id(), getType_code(), setType_code()는
    //     LedgerDTO에서 이미 정의됨 → 이 클래스에서 재정의 불필요.
    //   - 아래는 이 클래스만의 추가 필드에 대한 getter/setter만 정의.

    public int getRule_id()               { return rule_id; }
    public void setRule_id(int rule_id)   { this.rule_id = rule_id; }

    public long getAmount()               { return amount; }
    public void setAmount(long amount)    { this.amount = amount; }

    public String getTransaction_date()                       { return transaction_date; }
    public void setTransaction_date(String transaction_date)  { this.transaction_date = transaction_date; }
}
