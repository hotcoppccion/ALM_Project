-- ═══════════════════════════════════════════════════════════════════════
--  03_Ledger_Domain.sql  |  가계부 도메인 테이블 생성 (총 3개)
--
--  [테이블 구조]
--    ledger_category : 카테고리 목록 (식비, 교통비, 급여 등)
--    ledger_master   : 가계부 마스터 (모든 내역의 공통 부모)
--    general_ledger  : 일반 지출입 상세 (ledger_master 상속)
--
--  [자산 연동 구조]
--    general_ledger.asset_id → account_table (연동 계좌)
--    내역이 입력되면 Java Service 계층에서 해당 계좌의 balance 를 즉시 업데이트
--    → "가계부 등록 = 자산 잔액 실시간 반영" 트랜잭션 무결성 구현
--
--  [지출/수입 구분 방식]
--    별도 컬럼 없이 amount 의 부호로 구분합니다.
--    amount > 0 : 수입  (예: 급여 +3,000,000)
--    amount < 0 : 지출  (예: 식비 -50,000)
--    → Java 에서 양수/음수 처리만 하면 되므로 코드가 단순해집니다.
-- ═══════════════════════════════════════════════════════════════════════

USE alm_db;

-- ── 1. ledger_category — 카테고리 마스터 ─────────────────────────────
-- 지출과 수입 카테고리를 하나의 테이블에서 관리합니다.
-- is_default = TRUE  : 시스템 기본 제공 카테고리 (삭제 방지 권장)
-- is_default = FALSE : 사용자가 직접 추가한 커스텀 카테고리 (삭제 가능)
-- 초기 데이터는 08_Seed_LedgerCategories.sql 에서 삽입합니다.
--
-- BOOLEAN : MySQL 에서 BOOLEAN 은 내부적으로 TINYINT(1) 과 동일
--   TRUE = 1, FALSE = 0 으로 저장됩니다.
CREATE TABLE ledger_category (
    category_id   INT AUTO_INCREMENT PRIMARY KEY,   -- 카테고리 고유 ID
    category_name VARCHAR(50) NOT NULL,              -- 카테고리명 (예: "식비", "급여")
    is_default    BOOLEAN DEFAULT FALSE              -- 기본 제공 여부 (TRUE = 기본값)
);

-- ── 2. ledger_master — 가계부 마스터 (공통 부모) ─────────────────────
-- 자산 도메인의 asset_master 와 동일한 패턴 (Class Table Inheritance)
-- 모든 가계부 내역이 이 테이블에 먼저 행을 만들고 ledger_id 를 발급받습니다.
-- type_code : 현재는 'GEN'(일반 지출입) 하나만 사용.
--   향후 고정지출(FIX), 정기수입(REC) 등 확장을 고려한 설계입니다.
CREATE TABLE ledger_master (
    ledger_id  INT AUTO_INCREMENT PRIMARY KEY,       -- 가계부 내역 고유 ID
    type_code  VARCHAR(10) NOT NULL,                  -- 내역 유형: 현재 'GEN' 만 사용
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP    -- 생성 일시 (자동 기록)
);

-- ── 3. general_ledger — 일반 지출입 상세 (ledger_master 상속) ─────────
-- [PK = FK 동시 사용]
--   ledger_id 가 PRIMARY KEY 이면서 ledger_master 를 참조하는 FOREIGN KEY
--   → ledger_master 에 부모 행이 없으면 삽입 불가 (무결성 보장)
--
-- asset_id : NULL 허용
--   특정 계좌와 연동하지 않는 단순 메모 내역은 asset_id = NULL 로 저장
--   NULL 허용이므로 FOREIGN KEY 위반 없이 연동 없는 내역도 저장 가능
--
-- amount : BIGINT NOT NULL
--   양수 = 수입, 음수 = 지출. 원 단위 정수로 저장 (소수점 없음)
--
-- transaction_date : DATE 타입 (시간 없이 날짜만 저장)
--   예: '2025-05-20'  ← DATETIME 과 달리 시간 부분 없음
CREATE TABLE general_ledger (
    ledger_id        INT  PRIMARY KEY,               -- PK 겸 FK (ledger_master 참조)
    asset_id         INT,                             -- 연동 자산 (NULL = 계좌 미연동)
    category_id      INT,                             -- 카테고리 (ledger_category 참조)
    amount           BIGINT NOT NULL,                 -- 금액: 양수=수입 / 음수=지출
    transaction_date DATE   NOT NULL,                 -- 거래 날짜 (예: 2025-05-20)
    FOREIGN KEY (ledger_id)   REFERENCES ledger_master(ledger_id)   ON DELETE CASCADE,
    FOREIGN KEY (asset_id)    REFERENCES asset_master(asset_id),    -- NULL 허용 FK
    FOREIGN KEY (category_id) REFERENCES ledger_category(category_id)
);

SELECT '✅ 03 가계부 도메인 생성 완료 (3개 테이블: ledger_category, ledger_master, general_ledger)' AS result;
