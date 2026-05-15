package com.alm.dto;

/**
 * [DTO 계층 - 정기수입 영수증]
 * 정기수입 규칙이 실행되어 생성된 거래 내역(영수증)을 담는 DTO.
 * regular_income_receipt 테이블과 1:1로 매핑됨. 기획서 5.3절에 정의된 클래스.
 *
 * [상속 구조]
 *   LedgerDTO (abstract)
 *     └── RegularIncomeReceiptDTO  ← 이 클래스
 *
 * [Java 문법 개념] extends:
 *   - "extends LedgerDTO": 부모 클래스의 ledger_id, type_code 필드와 getter/setter를 상속.
 *   - 이 클래스는 부모 필드 외에 rule_id, amount, transaction_date 3개 필드만 추가 정의.
 *   - 상속 = 코드 재사용 + 공통 타입 분류 (LedgerDTO 타입으로 다형성 활용 가능).
 *
 * [DB 테이블: regular_income_receipt]
 *   - ledger_id         INT PK/FK (→ ledger_master)
 *   - rule_id           INT FK (→ regular_income_rule): 어느 규칙에서 생성됐는지
 *   - amount            BIGINT: 실제 수입 금액 (항상 양수)
 *   - transaction_date  DATE: 실제 입금일
 */
public class RegularIncomeReceiptDTO extends LedgerDTO {

    // 이 영수증을 생성한 정기수입 규칙의 ID. FK 참조.
    private int rule_id;

    // 실제 입금된 금액. 수입이므로 항상 양수.
    // 예: 월급 2,500,000원 → amount = 2500000
    private long amount;

    // 실제 입금일. "YYYY-MM-DD" 형식 문자열.
    private String transaction_date;

    // ── Getter / Setter ──────────────────────────────────────────────

    public int getRule_id()               { return rule_id; }
    public void setRule_id(int rule_id)   { this.rule_id = rule_id; }

    public long getAmount()               { return amount; }
    public void setAmount(long amount)    { this.amount = amount; }

    public String getTransaction_date()                       { return transaction_date; }
    public void setTransaction_date(String transaction_date)  { this.transaction_date = transaction_date; }
}
