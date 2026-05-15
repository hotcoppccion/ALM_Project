package com.alm.dto;

/**
 * [DTO 계층 - 변동지출 규칙]
 * 변동지출 규칙을 저장하는 variable_expense_rule 테이블과 1:1로 매핑되는 DTO.
 * 기획서 5.3절에 정의된 클래스.
 *
 * [설계 의도]
 *   - "변동지출 규칙": 고정금액이 아닌, 주기마다 금액을 직접 입력하는 지출 설정.
 *     예: 매달 말일 카드값 (금액 미리 알 수 없음) → 알림만 발생, 금액은 나중에 입력.
 *   - 고정지출 규칙(FixedExpenseRuleDTO)과의 차이: amount 필드가 없음.
 *     금액이 사전에 확정되지 않으므로 규칙 테이블에 amount를 저장하지 않음.
 *
 * [고정지출 vs 변동지출 필드 비교]
 *   FixedExpenseRuleDTO: rule_id, amount(O), base_date, p_value, p_unit
 *   VariableExpenseRuleDTO: rule_id,           base_date, p_value, p_unit
 *   → amount가 없다는 것이 핵심 차이.
 *
 * [Java 문법 개념] LedgerDTO 미상속:
 *   - 규칙(Rule) 계열 DTO는 거래 기록이 아닌 설정 정보이므로 ledger_master 참조 불필요.
 *   - 금액이 확정될 때 VariableExpenseReceiptDTO(영수증)가 생성되며, 영수증만 LedgerDTO를 상속.
 *
 * [DB 테이블: variable_expense_rule]
 *   - rule_id   INT AUTO_INCREMENT PK
 *   - base_date DATE
 *   - p_value   INT
 *   - p_unit    VARCHAR(10)
 */
public class VariableExpenseRuleDTO {

    // 변동지출 규칙의 Primary Key.
    private int rule_id;

    // 기준 날짜. 주기 계산의 시작점. "YYYY-MM-DD" 형식.
    private String base_date;

    // 주기 값. 예: 1(매달), 2(격월)
    private int p_value;

    // 주기 단위. 허용값: "DAY", "WEEK", "MONTH"
    private String p_unit;

    // ── Getter / Setter ──────────────────────────────────────────────
    // amount 필드가 없으므로 rule_id, base_date, p_value, p_unit 4개 필드만 정의.

    public int getRule_id()               { return rule_id; }
    public void setRule_id(int rule_id)   { this.rule_id = rule_id; }

    public String getBase_date()                  { return base_date; }
    public void setBase_date(String base_date)    { this.base_date = base_date; }

    public int getP_value()               { return p_value; }
    public void setP_value(int p_value)   { this.p_value = p_value; }

    public String getP_unit()               { return p_unit; }
    public void setP_unit(String p_unit)    { this.p_unit = p_unit; }
}
