-- ═══════════════════════════════════════════════════════════════════════
--  03_Ledger_Domain.sql  |  가계부 도메인 테이블 생성 (총 3개)
--
--  [테이블 구조]
--    ledger_category : 카테고리 목록 (식비, 교통비, 급여 등)
--    ledger_master   : 가계부 마스터 (Class Table Inheritance 부모)
--    general_ledger  : 일반 지출입 상세 (ledger_master 상속)
--
--  [자산 연동]
--    general_ledger.asset_id → asset_master (연동 계좌)
--    가계부 내역 등록 시 Java LedgerService 가 해당 자산의 balance 를 즉시 업데이트합니다.
--
--  [지출/수입 구분 방식]
--    별도 컬럼 없이 amount 의 부호로 구분합니다.
--    amount > 0 : 수입  /  amount < 0 : 지출
--    SUM() 한 번으로 순수지(net) 계산이 가능합니다.
-- ═══════════════════════════════════════════════════════════════════════

USE alm_db;

-- ── 1. ledger_category ────────────────────────────────────────────────
-- is_default = TRUE : 시스템 기본 제공 카테고리 (삭제 방지 권장)
-- is_default = FALSE: 사용자가 추가한 커스텀 카테고리 (삭제 가능)
CREATE TABLE ledger_category (
    category_id   INT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(50) NOT NULL,
    is_default    BOOLEAN DEFAULT FALSE
);

-- ── 2. ledger_master ─────────────────────────────────────────────────
-- 현재 type_code = 'GEN' (일반 수입/지출)만 사용합니다.
-- ledger_master 삭제 시 general_ledger 는 ON DELETE CASCADE 로 자동 삭제됩니다.
CREATE TABLE ledger_master (
    ledger_id  INT AUTO_INCREMENT PRIMARY KEY,
    type_code  VARCHAR(10) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ── 3. general_ledger — 일반 지출입 상세 ─────────────────────────────
-- [asset_id NULL 허용]
--   특정 계좌와 연동하지 않는 단순 메모 내역은 asset_id = NULL 로 저장합니다.
-- [amount BIGINT]
--   양수 = 수입, 음수 = 지출. 원 단위 정수 저장으로 부동소수점 오류를 방지합니다.
-- [asset_id FK — ON DELETE 미지정(RESTRICT)]
--   연동 자산 삭제 전 Java DeleteValidatorService 가 참조 여부를 확인하고 차단합니다.
--   FK 는 2차 방어선 역할을 합니다.
CREATE TABLE general_ledger (
    ledger_id        INT    PRIMARY KEY,
    asset_id         INT,                     -- 연동 자산 (NULL = 계좌 미연동)
    category_id      INT,
    amount           BIGINT NOT NULL,         -- 양수=수입 / 음수=지출
    transaction_date DATE   NOT NULL,
    FOREIGN KEY (ledger_id)   REFERENCES ledger_master(ledger_id)   ON DELETE CASCADE,
    FOREIGN KEY (asset_id)    REFERENCES asset_master(asset_id),
    FOREIGN KEY (category_id) REFERENCES ledger_category(category_id)
);

SELECT '✅ 03 가계부 도메인 생성 완료 (3개 테이블)' AS result;
