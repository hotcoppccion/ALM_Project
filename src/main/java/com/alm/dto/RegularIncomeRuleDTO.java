package com.alm.dto;

/**
 * [DTO 계층 - 정기수입 규칙]
 * 정기수입 규칙을 저장하는 regular_income_rule 테이블과 1:1로 매핑되는 DTO.
 * 기획서 5.3절에 정의된 클래스.
 *
 * [설계 의도]
 *   - "정기수입 규칙": 주기적으로 자동 처리될 수입 항목 설정 정보.
 *     예: 매달 25일 월급 250만원, 매주 월요일 아르바이트비 30만원.
 *   - FixedExpenseRuleDTO와 구조가 유사하지만 수입 방향(양수)을 나타냄.
 *
 * [Java 문법 개념] LedgerDTO 미상속:
 *   - 규칙(Rule)은 거래 기록이 아닌 설정 데이터이므로 ledger_master ID 체계 불필요.
 *   - 영수증 DTO(RegularIncomeReceiptDTO)가 LedgerDTO를 상속받아 ID를 가짐.
 *
 * [DB 테이블: regular_income_rule]
 *   - rule_id   INT AUTO_INCREMENT PK
 *   - amount    BIGINT   : 수입 금액 (항상 양수)
 *   - base_date DATE     : 최초 실행 기준일
 *   - p_value   INT      : 주기 값
 *   - p_unit    VARCHAR(10): 주기 단위 (DAY / WEEK / MONTH)
 *
 * [고정지출 규칙과의 차이]
 *   - FixedExpenseRuleDTO: amount가 지출용(Service에서 음수 처리).
 *   - RegularIncomeRuleDTO: amount가 수입용(Service에서 양수 그대로 처리).
 *   - 나머지 필드 구조는 동일 (주기 설정 공통).
 */
public class RegularIncomeRuleDTO {

    // 정기수입 규칙의 Primary Key.
    private int rule_id;

    // 수입 금액. 항상 양수. DB에도 양수로 저장됨.
    // 예: 매달 급여 2,500,000원 → amount = 2500000
    private long amount;

    // 주기 계산의 기준 날짜. "YYYY-MM-DD" 형식.
    private String base_date;

    // 주기 값. p_unit과 조합. 예: p_value=1, p_unit="MONTH" → 매달 반복.
    private int p_value;

    // 주기 단위. 허용값: "DAY", "WEEK", "MONTH"
    private String p_unit;

    // ── Getter / Setter ──────────────────────────────────────────────

    public int getRule_id()               { return rule_id; }
    public void setRule_id(int rule_id)   { this.rule_id = rule_id; }

    public long getAmount()               { return amount; }
    public void setAmount(long amount)    { this.amount = amount; }

    public String getBase_date()                  { return base_date; }
    public void setBase_date(String base_date)    { this.base_date = base_date; }

    public int getP_value()               { return p_value; }
    public void setP_value(int p_value)   { this.p_value = p_value; }

    public String getP_unit()               { return p_unit; }
    public void setP_unit(String p_unit)    { this.p_unit = p_unit; }
}
