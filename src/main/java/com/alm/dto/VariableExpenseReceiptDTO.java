package com.alm.dto;

/**
 * [DTO 계층 - 변동지출 영수증]
 * 변동지출 규칙이 실행된 후 사용자가 금액을 입력하여 확정된 거래 내역 DTO.
 * variable_expense_receipt 테이블과 1:1로 매핑됨. 기획서 5.3절에 정의된 클래스.
 *
 * [상속 구조]
 *   LedgerDTO (abstract)
 *     └── VariableExpenseReceiptDTO  ← 이 클래스
 *
 * [다른 영수증 DTO와의 차이점]
 *   - FixedExpenseReceiptDTO / RegularIncomeReceiptDTO: status 필드 없음.
 *   - VariableExpenseReceiptDTO: status 필드 있음.
 *
 * [status 필드 설명]
 *   - 변동지출은 규칙이 발동해도 금액이 미확정 상태(PENDING)로 생성됨.
 *   - 사용자가 금액을 입력하면 상태가 CONFIRMED로 변경됨.
 *   - 상태값(String): "PENDING"(대기 중), "CONFIRMED"(확정됨), "SKIPPED"(건너뜀)
 *   - Java enum 대신 String을 사용: DB VARCHAR 컬럼과 직접 매핑.
 *
 * [Java 문법 개념] extends:
 *   - LedgerDTO의 ledger_id, type_code 필드와 getter/setter를 상속받아 중복 없이 사용.
 *
 * [DB 테이블: variable_expense_receipt]
 *   - ledger_id         INT PK/FK (→ ledger_master)
 *   - rule_id           INT FK (→ variable_expense_rule)
 *   - amount            BIGINT: 사용자가 직접 입력한 금액 (지출이므로 음수 저장)
 *   - transaction_date  DATE
 *   - status            VARCHAR(20): "PENDING" / "CONFIRMED" / "SKIPPED"
 */
public class VariableExpenseReceiptDTO extends LedgerDTO {

    // 이 영수증을 생성한 변동지출 규칙의 ID. FK 참조.
    private int rule_id;

    // 사용자가 나중에 입력한 실제 지출 금액. 음수로 저장됨.
    // status가 "PENDING"일 때는 0 또는 임시값. "CONFIRMED" 시 실제 금액으로 갱신.
    private long amount;

    // 거래 발생일. "YYYY-MM-DD" 형식.
    private String transaction_date;

    // 처리 상태.
    // "PENDING"  : 규칙이 발동했으나 금액 미입력 상태 (사용자 액션 필요)
    // "CONFIRMED": 사용자가 금액을 입력하여 확정 완료
    // "SKIPPED"  : 이번 주기 지출이 발생하지 않아 건너뜀
    private String status;

    // ── Getter / Setter ──────────────────────────────────────────────

    public int getRule_id()               { return rule_id; }
    public void setRule_id(int rule_id)   { this.rule_id = rule_id; }

    public long getAmount()               { return amount; }
    public void setAmount(long amount)    { this.amount = amount; }

    public String getTransaction_date()                       { return transaction_date; }
    public void setTransaction_date(String transaction_date)  { this.transaction_date = transaction_date; }

    public String getStatus()               { return status; }
    public void setStatus(String status)    { this.status = status; }
}
