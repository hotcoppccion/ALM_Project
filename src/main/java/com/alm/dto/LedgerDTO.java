package com.alm.dto;

/**
 * 가계부 내역 DTO 추상 기반 클래스.
 * ledger_master 테이블과 1:1 대응. 하위 클래스(GeneralLedgerDTO 등)가 상속하여 사용.
 *
 * [Class Table Inheritance]
 *   ledger_master 는 전체 내역의 전역 ID(ledger_id) 를 발급하는 부모 테이블.
 *   실제 데이터는 general_ledger 등 자식 테이블에 저장되며,
 *   이 DTO 계층 구조는 그 DB 설계를 그대로 반영한다.
 */
public abstract class LedgerDTO {

    private int    ledger_id;  // ledger_master PK (AUTO_INCREMENT)
    private String type_code;  // 내역 유형: "GEN" (일반 수입/지출)

    public int    getLedger_id()                  { return ledger_id; }
    public void   setLedger_id(int ledger_id)     { this.ledger_id = ledger_id; }

    public String getType_code()                  { return type_code; }
    public void   setType_code(String type_code)  { this.type_code = type_code; }
}
