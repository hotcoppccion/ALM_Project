package com.alm.dto;

/**
 * [DTO 계층 - Abstract Class]
 * 모든 자산 DTO의 공통 필드(asset_id, type_code)를 정의하는 추상 최상위 클래스.
 *
 * [Java 문법 개념] abstract class (추상 클래스):
 *   - abstract 키워드가 붙은 클래스. new AssetDTO()로 직접 인스턴스 생성 불가.
 *   - 목적: 공통 필드/메서드를 한 곳에 정의하여 하위 클래스에서 상속받아 사용.
 *   - abstract 메서드(구현 없는 메서드)를 포함할 수 있으나, 이 클래스는 포함하지 않음.
 *     (추상 메서드 없는 추상 클래스도 허용 → 인스턴스화 방지 목적)
 *
 * [DB 매핑 - Class Table Inheritance 패턴]
 *   - asset_master 테이블과 1:1 대응.
 *   - asset_master: 전역 ID(asset_id)와 유형 코드(type_code)만 저장하는 부모 테이블.
 *   - 실제 자산 데이터는 account_table(ACC), real_estate(REA), physical_asset(PHY), cash_asset(CSH)에 분산 저장.
 *   - 이 추상 클래스가 DB의 부모 테이블(asset_master)을 Java 클래스로 표현함.
 *
 * [상속 관계]
 *   AssetDTO (abstract)
 *   ├── AccountDTO       (ACC 타입: 금융 계좌)
 *   ├── RealEstateDTO    (REA 타입: 부동산)
 *   ├── PhysicalAssetDTO (PHY 타입: 실물 자산)
 *   └── CashAssetDTO     (CSH 타입: 현금)
 */
public abstract class AssetDTO {

    // asset_master.asset_id: 전체 자산의 전역 고유 식별자.
    // AUTO_INCREMENT로 DB가 발급하는 정수. long 타입으로 최대 약 92경까지 수용 가능.
    // int(최대 약 21억)보다 long을 사용하는 이유: 대용량 시스템 확장성 및 안전성.
    private long asset_id;

    // asset_master.type_code: 자산의 유형을 나타내는 4자리 문자열 코드.
    // 값: "ACC"(금융계좌), "REA"(부동산), "PHY"(실물자산), "CSH"(현금)
    // String 사용 이유: DB VARCHAR(10) 컬럼과 직접 매핑. enum보다 DB 값 변경 시 코드 수정 최소화.
    private String type_code;

    // ── Getter / Setter ──────────────────────────────────────────────
    // [Java 문법 개념] 캡슐화(Encapsulation):
    //   - 필드를 private으로 선언하여 외부 직접 접근 차단.
    //   - getter: 읽기 전용 접근 제공. setter: 쓰기 접근 제공 (유효성 검사 로직 추가 가능).
    //   - Jackson 라이브러리: getter 이름에서 "get" 제거 + 소문자 → JSON 키로 사용.
    //     예: getAsset_id() → JSON 키 "asset_id"

    public long getAsset_id() { return asset_id; }
    public void setAsset_id(long asset_id) { this.asset_id = asset_id; }
    // this.asset_id: 현재 객체의 asset_id 필드를 명시적으로 지칭.
    // 파라미터 이름(asset_id)과 필드 이름(asset_id)이 같을 때 this로 구분.

    public String getType_code() { return type_code; }
    public void setType_code(String type_code) { this.type_code = type_code; }
}
