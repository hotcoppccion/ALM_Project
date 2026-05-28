package com.alm.dto;

/**
 * 모든 자산 DTO 의 공통 필드를 정의하는 추상 부모 클래스.
 *
 * [설계 근거 — 추상 클래스 선택]
 *   자산 타입(계좌/부동산/실물/현금)마다 전용 필드는 다르지만
 *   asset_id 와 type_code 는 공통이다. 추상 클래스로 선언해 공통 필드를 한 곳에 모으고
 *   직접 인스턴스화를 막아 "항상 구체 타입을 통해서만 사용"하도록 강제한다.
 *
 * [OCP / LSP — abstract getAmount()]
 *   기존: AssetService.calculateTotalAsset() 가 instanceof 체인으로 각 타입의 금액 필드를 직접 꺼냈다.
 *        새 타입을 추가할 때마다 Service 를 수정해야 했다 (OCP 위반).
 *   개선: 부모에 abstract getAmount() 를 선언하고 각 자식 DTO 가 구현한다.
 *        Service 는 dto.getAmount() 한 줄로 끝나며, 새 타입이 생겨도 Service 수정 불필요.
 */
public abstract class AssetDTO {

    // asset_master PK. DB AUTO_INCREMENT 값을 안전하게 담기 위해 long 사용.
    private long   asset_id;
    // "ACC" / "REA" / "PHY" / "CSH" — DB VARCHAR 값과 직접 매핑하므로 String.
    private String type_code;

    public long   getAsset_id()                  { return asset_id;  }
    public void   setAsset_id(long asset_id)     { this.asset_id = asset_id; }
    public String getType_code()                 { return type_code; }
    public void   setType_code(String type_code) { this.type_code = type_code; }

    /**
     * 자산의 현재 가치(원)를 반환한다.
     * 각 자식 DTO 가 자신의 금액 필드를 반환하도록 Override 한다.
     * - AccountDTO      → balance        (예수금 잔액)
     * - RealEstateDTO   → price          (부동산 가격)
     * - PhysicalAssetDTO→ current_value  (현재 평가액)
     * - CashAssetDTO    → balance        (현금 잔액)
     */
    public abstract long getAmount();
}
