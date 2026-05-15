package com.alm.dto;

/**
 * [DTO 계층 - 고정지출 규칙]
 * 고정지출 규칙을 저장하는 fixed_expense_rule 테이블과 1:1로 매핑되는 DTO.
 * 기획서 5.3절에 정의된 클래스.
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
 * [DB 테이블: fixed_expense_rule]
 *   - rule_id       INT AUTO_INCREMENT PK
 *   - amount        BIGINT             : 지출 금액 (항상 양수, 저장 시 음수 처리는 Service)
 *   - base_date     DATE               : 최초 실행 기준일
 *   - p_value       INT                : 주기 값 (예: 1, 2, 3)
 *   - p_unit        VARCHAR(10)        : 주기 단위 (DAY / WEEK / MONTH)
 */
public class FixedExpenseRuleDTO {

    // 고정지출 규칙의 Primary Key. DB가 AUTO_INCREMENT로 발급.
    // int 타입: 일반 규칙 수가 제한적이므로 32비트 정수로 충분.
    private int rule_id;

    // 지출 금액. 항상 양수로 저장 (실제 잔액 차감은 Service에서 부호 처리).
    // long 타입: 원화 금액이 int(약 21억) 한계를 초과할 가능성 고려.
    private long amount;

    // 주기 계산의 기준 날짜. "YYYY-MM-DD" 형식 문자열로 저장.
    // java.sql.Date 대신 String을 사용하는 이유:
    //   Jackson이 String → JSON 변환 시 추가 설정 없이 그대로 직렬화하므로 간단함.
    private String base_date;

    // 주기 값. p_unit과 함께 사용. 예: p_value=2, p_unit="WEEK" → 2주마다 반복.
    private int p_value;

    // 주기 단위. 허용값: "DAY", "WEEK", "MONTH"
    // Java enum 대신 String을 사용: DB VARCHAR 컬럼과 직접 매핑되어 별도 변환 불필요.
    private String p_unit;

    // ── Getter / Setter ──────────────────────────────────────────────
    // [Java 문법 개념] Java Bean 규약:
    //   - private 필드: 외부 직접 접근 차단 (캡슐화, Encapsulation).
    //   - public getter: 필드 값을 읽는 메서드. 반환 타입 = 필드 타입.
    //   - public setter: 필드 값을 쓰는 메서드. 반환 타입 void, 파라미터 = 필드 타입.
    //   - 메서드명 규칙: get/set + 필드명 (언더스코어 포함 그대로 사용).

    public int getRule_id()             { return rule_id; }
    public void setRule_id(int rule_id) { this.rule_id = rule_id; }

    public long getAmount()               { return amount; }
    public void setAmount(long amount)    { this.amount = amount; }

    public String getBase_date()                  { return base_date; }
    public void setBase_date(String base_date)    { this.base_date = base_date; }

    public int getP_value()               { return p_value; }
    public void setP_value(int p_value)   { this.p_value = p_value; }

    public String getP_unit()               { return p_unit; }
    public void setP_unit(String p_unit)    { this.p_unit = p_unit; }
}
