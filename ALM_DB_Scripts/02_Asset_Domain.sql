-- ═══════════════════════════════════════════════════════════════════════
--  02_Asset_Domain.sql  |  자산 도메인 테이블 생성 (총 7개)
--
--  [테이블 상속 패턴 — Class Table Inheritance]
--    객체지향의 상속 개념을 RDB(관계형 DB)로 표현하는 설계 패턴입니다.
--
--    ┌─ asset_master (부모: 공통 ID + 타입 코드)
--    │
--    ├─ account_table  (자식: 금융 계좌, type_code = 'ACC')
--    ├─ physical_asset (자식: 실물 자산, type_code = 'PHY')
--    ├─ real_estate    (자식: 부동산,   type_code = 'REA')
--    └─ cash_asset     (자식: 현금,     type_code = 'CSH')
--
--    asset_master 가 AUTO_INCREMENT 로 전역 ID 를 발급하면,
--    자식 테이블은 그 ID 를 자신의 PK 이자 FK 로 그대로 사용합니다.
--    → 어떤 자산이든 asset_id 하나로 어떤 테이블에 있는지 바로 조회 가능
--
--  [마스터 데이터 테이블 (계좌 도메인)]
--    account_bank  : 은행/금융사 목록 (KB국민, 카카오뱅크 등)
--    account_type  : 계좌 종류 목록 (입출금, 정기적금, CMA 등)
--    → 06_Seed_Banks.sql / 07_Seed_AccountTypes.sql 에서 초기값 삽입
-- ═══════════════════════════════════════════════════════════════════════

USE alm_db;

-- ── 1. asset_master — 자산 마스터 (모든 자산의 부모 테이블) ────────────
-- AUTO_INCREMENT : INSERT 시 자동으로 1씩 증가하는 정수 ID 생성
--   → 개발자가 ID 를 직접 관리할 필요 없음. DB 가 보장하는 유일성.
-- PRIMARY KEY    : 테이블 내 각 행을 고유하게 식별하는 컬럼
--   NOT NULL + UNIQUE 의 의미를 내포합니다.
-- DEFAULT CURRENT_TIMESTAMP : 행 삽입 시점의 날짜+시간을 자동 기록
--   → Java 코드에서 별도로 시간을 넘기지 않아도 됩니다.
CREATE TABLE asset_master (
    asset_id   INT AUTO_INCREMENT PRIMARY KEY,          -- 전역 자산 ID (자동 증가)
    type_code  VARCHAR(10)  NOT NULL,                   -- 자산 유형: ACC / PHY / REA / CSH
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP       -- 등록 일시 (자동 기록)
);

-- ── 2. account_bank — 은행/금융사 마스터 ─────────────────────────────
-- 계좌 등록 화면의 "은행 선택" 드롭다운 데이터 소스
-- 06_Seed_Banks.sql 에서 KB국민, 카카오뱅크, 키움증권 등 초기 데이터 삽입
CREATE TABLE account_bank (
    bank_id   INT AUTO_INCREMENT PRIMARY KEY,   -- 은행 고유 ID
    bank_name VARCHAR(50) NOT NULL              -- 은행명 (예: "카카오뱅크")
);

-- ── 3. account_type — 계좌 종류 마스터 ──────────────────────────────
-- 계좌 등록 화면의 "계좌 종류" 드롭다운 데이터 소스
-- 07_Seed_AccountTypes.sql 에서 보통예금, 정기적금, CMA 등 초기 데이터 삽입
-- ★ type_name = '증권위탁계좌' 인 경우 투자 포트폴리오와 연동됩니다.
CREATE TABLE account_type (
    type_id   INT AUTO_INCREMENT PRIMARY KEY,   -- 계좌 종류 고유 ID
    type_name VARCHAR(50) NOT NULL              -- 종류명 (예: "증권위탁계좌")
);

-- ── 4. account_table — 금융 계좌 (asset_master 상속) ─────────────────
-- [PK = FK 동시 사용 패턴]
--   asset_id 가 PRIMARY KEY 이면서 동시에 asset_master 를 참조하는 FOREIGN KEY
--   → asset_master 에 부모 행이 없으면 이 테이블에도 행을 삽입할 수 없음 (무결성 보장)
--
-- ON DELETE CASCADE : asset_master 의 행이 삭제될 때 이 테이블의 관련 행도 자동 삭제
--   → 자산 삭제 시 고아(orphan) 레코드가 남지 않음
--
-- DECIMAL(5,2) : 소수점 포함 숫자 타입. 전체 5자리 중 소수 2자리
--   예: 999.99 (이자율 최대 999.99%)
--   BIGINT 대신 DECIMAL 사용 이유: 이자율은 소수점이 있어서 정수형 부적합
--
-- BIGINT : 매우 큰 정수 저장 (-9경 ~ +9경)
--   잔액을 원(₩) 단위 정수로 저장. 소수점 없음 = 반올림 오류 없음
CREATE TABLE account_table (
    asset_id         INT PRIMARY KEY,               -- PK 겸 FK (asset_master 참조)
    bank_id          INT,                            -- 은행 (account_bank 참조, NULL 허용)
    type_id          INT,                            -- 계좌 종류 (account_type 참조, NULL 허용)
    acc_number       VARCHAR(50),                    -- 계좌번호 (예: "110-123-456789")
    balance          BIGINT DEFAULT 0,               -- 잔액 (원 단위 정수)
    account_interest DECIMAL(5,2) DEFAULT 0,         -- 이자율 (%)
    FOREIGN KEY (asset_id) REFERENCES asset_master(asset_id) ON DELETE CASCADE,
    FOREIGN KEY (bank_id)  REFERENCES account_bank(bank_id),
    FOREIGN KEY (type_id)  REFERENCES account_type(type_id)
);

-- ── 5. physical_asset — 실물 자산 (asset_master 상속) ────────────────
-- PC, 차량, 귀금속, 한정판 피규어 등 감가 / 가치 변동이 있는 자산
-- [설계 결정] 자동 감가상각 로직을 사용하지 않고
--   사용자가 current_value(현재 평가액)를 직접 입력/수정합니다.
--   이유: 품목마다 수명/가치 변동 방식이 달라 일률 계산이 부정확하기 때문
-- last_updated : 사용자가 평가액을 마지막으로 갱신한 날짜+시간 기록
CREATE TABLE physical_asset (
    asset_id       INT PRIMARY KEY,                  -- PK 겸 FK (asset_master 참조)
    item_name      VARCHAR(100) NOT NULL,             -- 품목명 (예: "MacBook Pro")
    purchase_price BIGINT DEFAULT 0,                  -- 최초 구입가 (원)
    current_value  BIGINT DEFAULT 0,                  -- 현재 평가액 (사용자 직접 관리)
    last_updated   DATETIME,                          -- 평가액 최종 갱신 일시
    FOREIGN KEY (asset_id) REFERENCES asset_master(asset_id) ON DELETE CASCADE
);

-- ── 6. real_estate — 부동산 (asset_master 상속) ───────────────────────
-- 자가, 전세, 월세, 분양권 등 부동산 자산 관리
-- contract_type : 계약 형태 구분 (전세 / 월세 / 매매 / 분양권)
-- price : 매매가 또는 전세 보증금 (원 단위 정수)
CREATE TABLE real_estate (
    asset_id      INT PRIMARY KEY,                   -- PK 겸 FK (asset_master 참조)
    contract_type VARCHAR(10),                        -- 계약 형태 (전세/월세/매매/분양권)
    address       VARCHAR(255),                       -- 주소 (예: "서울시 강남구 ...")
    price         BIGINT DEFAULT 0,                   -- 자산 금액 (원)
    FOREIGN KEY (asset_id) REFERENCES asset_master(asset_id) ON DELETE CASCADE
);

-- ── 7. cash_asset — 현금 자산 (asset_master 상속) ────────────────────
-- 지갑 현금, 금고, 달러 지폐 등 계좌 외 현금성 자산
-- name : 보관 장소/명칭 (예: "지갑", "달러 현금")
CREATE TABLE cash_asset (
    asset_id INT PRIMARY KEY,                        -- PK 겸 FK (asset_master 참조)
    name     VARCHAR(50),                             -- 현금 명칭/보관처
    balance  BIGINT DEFAULT 0,                        -- 보유 금액 (원)
    FOREIGN KEY (asset_id) REFERENCES asset_master(asset_id) ON DELETE CASCADE
);

SELECT '✅ 02 자산 도메인 생성 완료 (7개 테이블: asset_master, account_bank, account_type, account_table, physical_asset, real_estate, cash_asset)' AS result;
