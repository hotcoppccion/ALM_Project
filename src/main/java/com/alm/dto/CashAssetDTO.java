package com.alm.dto;

/**
 * 현금성 자산(cash_asset) DTO.
 *
 * [설계 근거 — AccountDTO 와 분리]
 *   지갑 현금, 파킹통장 등 은행·계좌번호·이자율 정보 없이
 *   "이름 + 잔액"만 관리하는 단순 자산 타입.
 *   AccountDTO 의 서브타입으로 합치면 불필요한 NULL 필드가 생기므로 별도 테이블·DTO 로 분리.
 */
public class CashAssetDTO extends AssetDTO {

    private String name;
    private long   balance;

    public String getName()             { return name; }
    public void   setName(String v)     { this.name = v; }
    public long   getBalance()          { return balance; }
    public void   setBalance(long v)    { this.balance = v; }

    @Override
    public long getAmount() { return balance; }
}
