package com.alm.dto;

/**
 * [DTO 계층 - Abstract Class]
 * 모든 가계부 내역(영수증) DTO의 공통 필드를 정의하는 추상 클래스.
 *
 * [Java 문법 개념] abstract class:
 *   - abstract 키워드가 붙은 클래스는 직접 인스턴스화(new LedgerDTO()) 불가능.
 *   - 반드시 하위 클래스(GeneralLedgerDTO 등)에서 extends로 상속받아 사용해야 함.
 *   - 공통 필드를 한 곳에 정의하여 코드 중복을 제거하는 OOP 상속 설계 패턴.
 *
 * [DB 매핑]
 *   - ledger_master 테이블과 1:1 대응.
 *   - ledger_master는 전체 가계부 내역의 전역 ID를 발급하는 부모 테이블.
 *   - 실제 데이터는 general_ledger, fixed_expense_receipt 등 자식 테이블에 저장됨.
 *   - 이 구조를 Class Table Inheritance(식별 관계 상속) 패턴이라 부름.
 */
public abstract class LedgerDTO {

    // ledger_master 테이블의 Primary Key.
    // int 타입: AUTO_INCREMENT로 DB가 자동 발급. asset_id와 달리 int(32bit)로 충분.
    private int ledger_id;

    // 내역 유형 구분 코드. DB VARCHAR(20) 컬럼에 저장되는 문자열 상수.
    // 사용 값: "GEN"(일반 지출입), "FIX_R"(고정지출), "REG_R"(정기수입), "VAR_R"(변동지출)
    // Java enum 대신 String을 사용하여 DB와의 직접 매핑 및 확장성을 확보함.
    private String type_code;

    // ── Getter / Setter ──────────────────────────────────────────────
    // [Java 문법 개념] Java Bean 규약:
    //   - private 필드를 외부에서 읽으려면 get필드명(), 쓰려면 set필드명() 메서드를 정의.
    //   - 이 규약을 따르면 Spring의 JSON 직렬화(Jackson) 라이브러리가 자동으로 필드를 처리함.
    //   - 메서드 이름: get + 필드명(첫 글자 대문자). 예: ledger_id → getLedger_id()

    public int getLedger_id() { return ledger_id; }
    public void setLedger_id(int ledger_id) { this.ledger_id = ledger_id; }

    public String getType_code() { return type_code; }
    public void setType_code(String type_code) { this.type_code = type_code; }
}
