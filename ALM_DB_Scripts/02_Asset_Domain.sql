-- ═══════════════════════════════════════════════════════════════════════
--  02_Asset_Domain.sql  |  자산 도메인 테이블 생성 (총 7개)
--
--  [Class Table Inheritance]
--    asset_master 가 AUTO_INCREMENT 로 전역 asset_id 를 발급하고,
--    자식 테이블(account_table / physical_asset / real_estate / cash_asset)은
--    그 ID 를 자신의 PK 이자 FK 로 그대로 사용합니다.
--
--    ┌─ asset_master    (부모: 공통 ID + 타입 코드)
--    ├─ account_table   (ACC: 금융 계좌)
--    ├─ physical_asset  (PHY: 실물 자산)
--    ├─ real_estate     (REA: 부동산)
--    └─ cash_asset      (CSH: 현금)
--
--    자식 테이블 전체에 ON DELETE CASCADE 적용 →
--    asset_master 1건 삭제만으로 자식 행까지 자동 제거됩니다.
--
--  [마스터 데이터]
--    account_bank / account_type 은 드롭다운 목록용.
--    실제 데이터는 06_Seed_Banks.sql / 07_Seed_AccountTypes.sql 에서 삽입합니다.
-- ═══════════════════════════════════════════════════════════════════════

USE alm_db;

-- ── 1. asset_master ───────────────────────────────────────────────────
CREATE TABLE asset_master (
    asset_id   INT AUTO_INCREMENT PRIMARY KEY,
    type_code  VARCHAR(10)  NOT NULL,                   -- ACC / PHY / REA / CSH
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ── 2. account_bank — 은행/금융사/증권사 마스터 ───────────────────────
-- 은행 계좌와 증권 계좌 모두 "어느 기관에 개설된 계좌인가?" 를 나타내므로 통합 관리
CREATE TABLE account_bank (
    bank_id   INT AUTO_INCREMENT PRIMARY KEY,
    bank_name VARCHAR(50) NOT NULL
);

-- ── 3. account_type — 계좌 종류 마스터 ──────────────────────────────
-- ★ type_name = '증권위탁계좌' 는 투자 포트폴리오 연동 대상 계좌를 식별하는 기준입니다.
--   Java InvestRepository.findBrokerageAccounts() 에서 이 문자열로 계좌를 필터링합니다.
--   임의로 수정 시 투자 기능이 동작하지 않습니다.
CREATE TABLE account_type (
    type_id   INT AUTO_INCREMENT PRIMARY KEY,
    type_name VARCHAR(50) NOT NULL
);

-- ── 4. account_table — 금융 계좌 ────────────────────────────────────
-- bank_id / type_id : NULL 허용 — 등록 시 은행/종류를 미지정할 수 있습니다.
-- balance           : 원 단위 정수. 소수점 없음으로 부동소수점 오류를 방지합니다.
-- account_interest  : 이자율(%). DECIMAL(5,2) — 소수점 있는 금융 수치이므로 정수형 부적합.
CREATE TABLE account_table (
    asset_id         INT PRIMARY KEY,
    bank_id          INT,
    type_id          INT,
    acc_number       VARCHAR(50),
    balance          BIGINT      DEFAULT 0,
    account_interest DECIMAL(5,2) DEFAULT 0,
    FOREIGN KEY (asset_id) REFERENCES asset_master(asset_id) ON DELETE CASCADE,
    FOREIGN KEY (bank_id)  REFERENCES account_bank(bank_id),
    FOREIGN KEY (type_id)  REFERENCES account_type(type_id)
);

-- ── 5. physical_asset — 실물 자산 ────────────────────────────────────
-- 자동 감가상각 로직 미적용. 품목마다 가치 변동 방식이 달라 일률 계산이 부정확하기 때문.
-- 사용자가 current_value(현재 평가액)를 직접 수정합니다.
CREATE TABLE physical_asset (
    asset_id       INT PRIMARY KEY,
    item_name      VARCHAR(100) NOT NULL,
    purchase_price BIGINT DEFAULT 0,       -- 최초 구입가 (원)
    current_value  BIGINT DEFAULT 0,       -- 현재 평가액 (사용자 직접 관리)
    last_updated   DATETIME,
    FOREIGN KEY (asset_id) REFERENCES asset_master(asset_id) ON DELETE CASCADE
);

-- ── 6. real_estate — 부동산 ──────────────────────────────────────────
CREATE TABLE real_estate (
    asset_id      INT PRIMARY KEY,
    contract_type VARCHAR(10),     -- 전세 / 월세 / 매매 / 분양권
    address       VARCHAR(255),
    price         BIGINT DEFAULT 0,
    FOREIGN KEY (asset_id) REFERENCES asset_master(asset_id) ON DELETE CASCADE
);

-- ── 7. cash_asset — 현금 자산 ────────────────────────────────────────
CREATE TABLE cash_asset (
    asset_id INT PRIMARY KEY,
    name     VARCHAR(50),          -- 보관처 (예: "지갑", "달러 현금")
    balance  BIGINT DEFAULT 0,
    FOREIGN KEY (asset_id) REFERENCES asset_master(asset_id) ON DELETE CASCADE
);

SELECT '✅ 02 자산 도메인 생성 완료 (7개 테이블)' AS result;
