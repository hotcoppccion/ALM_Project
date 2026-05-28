package com.alm.dto;

/**
 * 실물자산(physical_asset) DTO.
 *
 * [설계 근거 — purchase_price / current_value 분리]
 *   매입가는 불변(이력), 현재 평가액은 시세에 따라 변동한다.
 *   두 값을 분리 저장하면 손익(current_value - purchase_price)과 수익률 계산이 가능하다.
 *   현재가 API 연동이 없으므로 사용자가 current_value 를 수동 업데이트한다.
 */
public class PhysicalAssetDTO extends AssetDTO {

    private String        item_name;
    private long          purchase_price; // 최초 매입가 (불변)
    private long          current_value;  // 현재 평가액 (사용자 수동 입력)
    private java.sql.Date last_updated;   // DB NOW() 로 자동 갱신, Java 에서는 참조만

    public String        getItem_name()                    { return item_name; }
    public void          setItem_name(String v)            { this.item_name = v; }
    public long          getPurchase_price()               { return purchase_price; }
    public void          setPurchase_price(long v)         { this.purchase_price = v; }
    public long          getCurrent_value()                { return current_value; }
    public void          setCurrent_value(long v)          { this.current_value = v; }
    public java.sql.Date getLast_updated()                 { return last_updated; }
    public void          setLast_updated(java.sql.Date v)  { this.last_updated = v; }

    // 총자산 산정 시 현재 가치 기준 사용. 매입가로 계산하면 평가 손실이 반영 안 됨.
    @Override
    public long getAmount() { return current_value; }
}
