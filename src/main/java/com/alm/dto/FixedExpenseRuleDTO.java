package com.alm.dto;

/**
 * [DTO 계층 - 고정지출 규칙]
 * 고정지출 규칙을 저장하는 fixed_expense_rule 테이블과 1:1로 매핑되는 DTO.
 *
 * [설계 의도]
 *   - "고정지출 규칙": 매월 자동으로 반복 처리될 지출 항목의 설정 정보.
 *     예: 매달 1일 월세 50만원, 매달 5일 인터넷 요금 2만 7천원.
 *   - 규칙(Rule)이 발동하면 → FixedExpenseReceiptDTO(영수증)가 생성됨.
 *
 * [Java 문법 개념] 상속(Inheritance) 미적용 이유:
 *   - LedgerDTO를 상속받지 않음. 규칙(Rule)은 거래 내역(Receipt)이 아니라
 *     설정 데이터이므로 ledger_master의 ID 체계를 공유하지 않음.
 *   - 영수증 DTO(FixedExpenseReceiptDTO)만 LedgerDTO를 상속받아 ledger_id를 가짐.
 *
 * [DB 테이블: fixed_expense_rule] (10_Alter_Rule_Tables.sql 적용 후)
 *   - rule_id       INT AUTO_INCREMENT PK
 *   - name          VARCHAR(100) NOT NULL DEFAULT '미지정' : 규칙 이름
 *   - category_id   INT NULL FK → ledger_category
 *   - amount        BIGINT             : 지출 금액 (항상 양수)
 *   - base_date     DATE               : 최초 실행 기준일
 *   - p_value       INT                : 주기 값 (예: 1, 2, 3)
 *   - p_unit        VARCHAR(10)        : 주기 단위 (DAY / WEEK / MONTH)
 */
public class FixedExpenseRuleDTO {

    // 고정지출 규칙의 Primary Key. DB가 AUTO_INCREMENT로 발급.
    private int rule_id;

    // 규칙 이름. 사용자가 직접 입력하는 라벨. 예: "월세", "OTT 구독".
    private String name;

    // 카테고리 식별자. ledger_category.category_id 참조 FK. NULL 허용.
    private int category_id;

    // 지출 금액. 항상 양수로 저장 (실제 잔액 차감은 Service에서 부호 처리).
    // long 타입: 원화 금액이 int(약 21억) 한계를 초과할 가능성 고려.
    private long amount;

    // 주기 계산의 기준 날짜. "YYYY-MM-DD" 형식 문자열로 저장.
    private String base_date;

    // 주기 값. p_unit과 함께 사용. 예: p_value=2, p_unit="WEEK" → 2주마다 반복.
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
