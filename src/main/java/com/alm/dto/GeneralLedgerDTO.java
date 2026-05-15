package com.alm.dto;

/**
 * [DTO 계층 - Concrete Class]
 * 사용자가 직접 입력하는 일반 지출/수입 내역을 계층 간에 전달하는 데이터 운반 객체.
 *
 * [Java 문법 개념] extends (상속):
 *   - LedgerDTO를 상속받음 → ledger_id, type_code 필드를 그대로 물려받음.
 *   - "is-a" 관계: GeneralLedgerDTO는 LedgerDTO의 한 종류.
 *   - 이 클래스는 abstract가 아니므로 직접 new GeneralLedgerDTO() 인스턴스화 가능.
 *
 * [DB 매핑]
 *   - general_ledger 테이블과 1:1 대응.
 *   - ledger_id는 ledger_master에서 먼저 발급받은 후, 동일한 ID를 general_ledger에도 사용.
 *
 * [금액 부호 규약 (Sign Convention)]
 *   - amount > 0 : 수입 (income) → 연동 자산의 잔액 증가
 *   - amount < 0 : 지출 (expense) → 연동 자산의 잔액 감소
 *   - 이 규약으로 단일 컬럼으로 수입/지출을 구분하고, SUM() 하나로 순수지(net)를 계산 가능.
 */
public class GeneralLedgerDTO extends LedgerDTO {

    // general_ledger.asset_id: 이 거래가 연동된 자산의 전역 식별자.
    // Foreign Key → asset_master.asset_id. 자산 잔액 업데이트 시 이 ID로 해당 자산을 찾음.
    // long 타입: asset_master의 asset_id가 AUTO_INCREMENT BIGINT이므로 long으로 수용.
    private long asset_id;

    // general_ledger.category_id: 이 거래가 속하는 카테고리의 식별자.
    // Foreign Key → ledger_category.category_id. 예: 식비=1, 교통비=2, 급여=11 등.
    // int 타입: 카테고리 수가 많지 않아 32비트 정수로 충분.
    private int category_id;

    // general_ledger.amount: 거래 금액 (부호 포함).
    // DB DECIMAL(18,2) → Java long 매핑. 원화(KRW)는 소수점이 없으므로 long으로 관리.
    // DECIMAL(18,2)는 소수점 아래 2자리까지 저장 가능하지만, 본 시스템은 정수 단위만 사용.
    private long amount;

    // general_ledger.transaction_date: 거래 발생일자. "YYYY-MM-DD" 형식의 문자열.
    // [Java 문법 개념] 날짜 타입 선택:
    //   - java.time.LocalDate: Java 8 이후 표준 날짜 타입. 시간 정보 없는 순수 날짜.
    //   - 여기서는 String으로 관리하여 JSON 직렬화/역직렬화 시 별도 포맷터 없이 처리.
    //   - DB PreparedStatement에서 java.sql.Date.valueOf(this.transaction_date)로 변환.
    private String transaction_date;

    // ── 아래 2개 필드는 DB general_ledger 테이블에 저장되지 않는 임시 필드 ──
    // SQL JOIN 결과를 Java 객체에 담기 위한 용도. DB 컬럼과 1:1 대응하지 않음.

    // ledger_category.category_name: JOIN으로 category_id → 이름 문자열로 변환한 값.
    // 화면 표출용. 예: category_id=1 → category_name="식비"
    private String category_name;

    // 연동 자산의 표시명: JOIN으로 가져온 자산 식별 문자열.
    // ACC 타입 → "은행명 (계좌번호 끝 4자리)". 예: "카카오뱅크 (4567)"
    // CSH 타입 → cash_asset.name 필드값. 예: "지갑 현금"
    private String asset_name;

    // ── Getter / Setter ──────────────────────────────────────────────
    // [Java 문법 개념] 단일 책임 원칙(SRP):
    //   - DTO 클래스는 데이터 운반만 담당하고, 비즈니스 로직은 포함하지 않음.
    //   - getter: 필드값 반환 (read). setter: 필드값 설정 (write).
    //   - Jackson 라이브러리는 getter 이름에서 "get"을 제거하고 소문자로 바꾼 이름을 JSON 키로 사용.
    //     예: getAmount() → JSON 키 "amount"

    public long getAsset_id() { return asset_id; }
    public void setAsset_id(long asset_id) { this.asset_id = asset_id; }

    public int getCategory_id() { return category_id; }
    public void setCategory_id(int category_id) { this.category_id = category_id; }

    public long getAmount() { return amount; }
    public void setAmount(long amount) { this.amount = amount; }

    public String getTransaction_date() { return transaction_date; }
    public void setTransaction_date(String transaction_date) { this.transaction_date = transaction_date; }

    public String getCategory_name() { return category_name; }
    public void setCategory_name(String category_name) { this.category_name = category_name; }

    public String getAsset_name() { return asset_name; }
    public void setAsset_name(String asset_name) { this.asset_name = asset_name; }
}
