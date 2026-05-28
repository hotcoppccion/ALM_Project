package com.alm.dto;

/**
 * 일반 지출/수입 내역 DTO.
 * general_ledger 테이블과 1:1 대응. LedgerDTO(ledger_id, type_code) 상속.
 *
 * [금액 부호 규약]
 *   amount > 0 : 수입 → 연동 자산 잔액 증가
 *   amount < 0 : 지출 → 연동 자산 잔액 감소
 *   단일 컬럼으로 수입/지출을 구분하고, SUM() 한 번으로 순수지(net) 계산 가능.
 *
 * [category_name, asset_name]
 *   DB 컬럼이 아닌 JOIN 결과 임시 필드. 화면 표출 전용.
 *   asset_name : ACC → "은행명 (계좌번호 끝 4자리)", CSH → cash_asset.name
 */
public class GeneralLedgerDTO extends LedgerDTO {

    private long   asset_id;          // 연동 자산 ID (asset_master.asset_id FK)
    private int    category_id;       // 카테고리 ID (ledger_category.category_id FK)
    private long   amount;            // 거래 금액 (부호 포함, 원 단위 정수)
    private String transaction_date;  // 거래 발생일자 (YYYY-MM-DD 문자열)

    // ── JOIN 결과 임시 필드 (DB 컬럼 아님) ───────────────────────────
    private String category_name;
    private String asset_name;

    // ── Getter / Setter ──────────────────────────────────────────────
    public long   getAsset_id()                          { return asset_id; }
    public void   setAsset_id(long asset_id)             { this.asset_id = asset_id; }

    public int    getCategory_id()                       { return category_id; }
    public void   setCategory_id(int category_id)        { this.category_id = category_id; }

    public long   getAmount()                            { return amount; }
    public void   setAmount(long amount)                 { this.amount = amount; }

    public String getTransaction_date()                  { return transaction_date; }
    public void   setTransaction_date(String v)          { this.transaction_date = v; }

    public String getCategory_name()                     { return category_name; }
    public void   setCategory_name(String category_name) { this.category_name = category_name; }

    public String getAsset_name()                        { return asset_name; }
    public void   setAsset_name(String asset_name)       { this.asset_name = asset_name; }
}
