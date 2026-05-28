package com.alm.dto;

/**
 * 부동산(real_estate) DTO.
 *
 * [설계 근거 — 단일 price 필드]
 *   자가(시장가) / 전세(보증금) / 월세(보증금) 모두 price 하나로 표현한다.
 *   각 계약 유형별로 "현재 자산 가치"에 해당하는 금액이 다르지만,
 *   단순화를 위해 사용자가 직접 해당 금액을 입력하도록 설계했다.
 *   월세의 월 납부액은 가계부(지출)에서 별도 관리한다.
 */
public class RealEstateDTO extends AssetDTO {

    private String contract_type; // "자가" / "전세" / "월세" / "분양권"
    private String address;
    private long   price;         // 자가: 시장가 / 전세·월세: 보증금

    public String getContract_type()              { return contract_type; }
    public void   setContract_type(String v)      { this.contract_type = v; }
    public String getAddress()                    { return address; }
    public void   setAddress(String v)            { this.address = v; }
    public long   getPrice()                      { return price; }
    public void   setPrice(long v)                { this.price = v; }

    @Override
    public long getAmount() { return price; }
}
