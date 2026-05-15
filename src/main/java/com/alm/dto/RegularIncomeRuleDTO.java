package com.alm.dto;

/**
 * [DTO 계층 - 정기수입 규칙]
 * 정기수입 규칙을 저장하는 regular_income_rule 테이블과 1:1로 매핑되는 DTO.
 *
 * [설계 의도]
 *   - "정기수입 규칙": 월급, 용돈 등 주기적으로 입금되는 수입 항목의 설정 정보.
 *   - 규칙(Rule)이 발동하면 → RegularIncomeReceiptDTO(영수증)가 생성됨.
 *   - FixedExpenseRuleDTO와 구조가 동일하지만 수입 방향(양수)을 나타냄.
 *
 * [DB 테이블: regular_income_rule] (10_Alter_Rule_Tables.sql 적용 후)
 *   - rule_id       INT AUTO_INCREMENT PK
 *   - name          VARCHAR(100) NOT NULL DEFAULT '미지정' : 규칙 이름
 *   - category_id   INT NULL FK → ledger_category
 *   - amount        BIGINT             : 수입 금액 (항상 양수)
 *   - base_date     DATE               : 최초 실행 기준일
 *   - p_value       INT                : 주기 값 (예: 1, 2, 3)
 *   - p_unit        VARCHAR(10)        : 주기 단위 (DAY / WEEK / MONTH)
 */
public class RegularIncomeRuleDTO {

    // 정기수입 규칙의 Primary Key. DB가 AUTO_INCREMENT로 발급.
    private int rule_id;

    // 규칙 이름. 사용자가 직접 입력하는 라벨. 예: "월급", "용돈".
    private String name;

    // 카테고리 식별자. ledger_category.category_id 참조 FK. NULL 허용.
    private int category_id;

    // 수입 금액. 항상 양수로 저장 (실제 잔액 증가는 Service에서 처리).
    // 예: 매달 급여 2,500,000원 → amount = 2500000
    private long amount;

    // 주기 계산의 기준 날짜. "YYYY-MM-DD" 형식 문자열로 저장.
    private String base_date;

    // 주기 값. p_unit과 함께 사용. 예: p_value=1, p_unit="MONTH" → 매달 반복.
    private int p_value;

    // 주기 단위. 허용값: "DAY", "WEEK", "MONTH"
    private String p_unit;

    // ── JOIN 표시용 필드 (DB 컬럼에 저장되지 않음) ──────────────
    // LEFT JOIN ledger_category 결과를 담는 임시 필드.
    private String category_name;

    // ── Getter / Setter ──────────────────────────────────────────────

    public int getRule_id()             { return rule_id; }
    public void setRule_id(int rule_id) { this.rule_id = rule_id; }

    public String getName()              { return name; }
    public void setName(String name)     { this.name = name; }

    public int getCategory_id()                   { return category_id; }
    public void setCategory_id(int category_id)   { this.category_id = category_id; }

    public long getAmount()               { return amount; }
    public void setAmount(long amount)    { this.amount = amount; }

    public String getBase_date()                  { return base_date; }
    public void setBase_date(String base_date)    { this.base_date = base_date; }

    public int getP_value()               { return p_value; }
    public void setP_value(int p_value)   { this.p_value = p_value; }

    public String getP_unit()               { return p_unit; }
    public void setP_unit(String p_unit)    { this.p_unit = p_unit; }

    public String getCategory_name()                    { return category_name; }
    public void setCategory_name(String category_name)  { this.category_name = category_name; }
}
