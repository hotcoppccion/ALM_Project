package com.alm.dto;

/**
 * [DTO 계층 - 변동지출 규칙]
 * 변동지출 규칙을 저장하는 variable_expense_rule 테이블과 1:1로 매핑되는 DTO.
 *
 * [설계 의도]
 *   - "변동지출 규칙": 주기는 일정하나 금액이 매번 달라지는 지출 설정 정보.
 *     예: 가스비, 전기세 — 매달 발생하지만 금액은 청구서를 받아야 알 수 있음.
 *   - FixedExpenseRuleDTO와 달리 amount 필드가 없음 (금액은 영수증에서 결정).
 *
 * [DB 테이블: variable_expense_rule] (10_Alter_Rule_Tables.sql 적용 후)
 *   - rule_id       INT AUTO_INCREMENT PK
 *   - name          VARCHAR(100) NOT NULL DEFAULT '미지정' : 규칙 이름
 *   - category_id   INT NULL FK → ledger_category
 *   - base_date     DATE               : 최초 실행 기준일
 *   - p_value       INT                : 주기 값
 *   - p_unit        VARCHAR(10)        : 주기 단위 (DAY / WEEK / MONTH)
 *   (amount 컬럼 없음 — 변동지출의 핵심 특징)
 */
public class VariableExpenseRuleDTO {

    // 변동지출 규칙의 Primary Key. DB가 AUTO_INCREMENT로 발급.
    private int rule_id;

    // 규칙 이름. 사용자가 직접 입력하는 라벨. 예: "가스비", "전기세".
    private String name;

    // 카테고리 식별자. ledger_category.category_id 참조 FK. NULL 허용.
    private int category_id;

    // 기준 날짜. 주기 계산의 시작점. "YYYY-MM-DD" 형식.
    private String base_date;

    // 주기 값. 예: 1(매달), 2(격월)
    private int p_value;

    // 주기 단위. 허용값: "DAY", "WEEK", "MONTH"
    private String p_unit;

    // ── JOIN 표시용 필드 (DB 컬럼에 저장되지 않음) ──────────────
    // LEFT JOIN ledger_category 결과를 담는 임시 필드.
    private String category_name;

    // ── Getter / Setter ──────────────────────────────────────────────
    // amount 필드가 없으므로 rule_id, name, category_id, base_date, p_value, p_unit 정의.

    public int getRule_id()               { return rule_id; }
    public void setRule_id(int rule_id)   { this.rule_id = rule_id; }

    public String getName()              { return name; }
    public void setName(String name)     { this.name = name; }

    public int getCategory_id()                   { return category_id; }
    public void setCategory_id(int category_id)   { this.category_id = category_id; }

    public String getBase_date()                  { return base_date; }
    public void setBase_date(String base_date)    { this.base_date = base_date; }

    public int getP_value()               { return p_value; }
    public void setP_value(int p_value)   { this.p_value = p_value; }

    public String getP_unit()               { return p_unit; }
    public void setP_unit(String p_unit)    { this.p_unit = p_unit; }

    public String getCategory_name()                    { return category_name; }
    public void setCategory_name(String category_name)  { this.category_name = category_name; }
}
