-- ═══════════════════════════════════════════════════════════
--  10_Alter_Rule_Tables.sql  (DROP + 재생성 방식 / MySQL 8.0)
--
--  규칙 테이블 3종 + 영수증 테이블 3종을 삭제 후 재생성.
--  name(규칙명), category_id(FK) 컬럼이 처음부터 포함된 구조.
--
--  실행 방법:
--    1. 좌측 SCHEMAS에서 alm_db 더블클릭 → 볼드체 확인
--    2. 이 파일 전체 선택(Ctrl+A)
--    3. 번개 버튼(⚡ Execute All) 또는 Ctrl+Shift+Enter
--
--  주의: 기존 규칙/영수증 데이터가 삭제됩니다.
--        (아직 데이터가 없다면 문제없음)
-- ═══════════════════════════════════════════════════════════

USE alm_db;

-- FK 체크 일시 해제 (참조 관계 무시하고 DROP 허용)
SET FOREIGN_KEY_CHECKS = 0;

-- ──────────────────────────────────────────────────────────
--  영수증 테이블 삭제 (규칙 테이블을 FK로 참조하므로 먼저 삭제)
-- ──────────────────────────────────────────────────────────
DROP TABLE IF EXISTS fixed_expense_receipt;
DROP TABLE IF EXISTS regular_income_receipt;
DROP TABLE IF EXISTS variable_expense_receipt;

-- ──────────────────────────────────────────────────────────
--  규칙 테이블 삭제
-- ──────────────────────────────────────────────────────────
DROP TABLE IF EXISTS fixed_expense_rule;
DROP TABLE IF EXISTS regular_income_rule;
DROP TABLE IF EXISTS variable_expense_rule;

-- ──────────────────────────────────────────────────────────
--  규칙 테이블 재생성 (name, category_id 컬럼 포함)
-- ──────────────────────────────────────────────────────────

CREATE TABLE fixed_expense_rule (
    rule_id     INT AUTO_INCREMENT PRIMARY KEY,
    `name`      VARCHAR(100) NOT NULL DEFAULT '미지정',
    category_id INT NULL,
    amount      BIGINT NOT NULL,
    base_date   DATE NOT NULL,
    p_value     INT NOT NULL,
    p_unit      VARCHAR(10) NOT NULL,
    FOREIGN KEY (category_id) REFERENCES ledger_category(category_id)
);

CREATE TABLE regular_income_rule (
    rule_id     INT AUTO_INCREMENT PRIMARY KEY,
    `name`      VARCHAR(100) NOT NULL DEFAULT '미지정',
    category_id INT NULL,
    amount      BIGINT NOT NULL,
    base_date   DATE NOT NULL,
    p_value     INT NOT NULL,
    p_unit      VARCHAR(10) NOT NULL,
    FOREIGN KEY (category_id) REFERENCES ledger_category(category_id)
);

CREATE TABLE variable_expense_rule (
    rule_id     INT AUTO_INCREMENT PRIMARY KEY,
    `name`      VARCHAR(100) NOT NULL DEFAULT '미지정',
    category_id INT NULL,
    base_date   DATE NOT NULL,
    p_value     INT NOT NULL,
    p_unit      VARCHAR(10) NOT NULL,
    FOREIGN KEY (category_id) REFERENCES ledger_category(category_id)
);

-- ──────────────────────────────────────────────────────────
--  영수증 테이블 재생성 (규칙 테이블 재생성 후 FK 재연결)
-- ──────────────────────────────────────────────────────────

CREATE TABLE fixed_expense_receipt (
    ledger_id        INT PRIMARY KEY,
    asset_id         BIGINT,
    category_id      INT,
    rule_id          INT,
    amount           BIGINT NOT NULL,
    transaction_date DATE NOT NULL,
    FOREIGN KEY (ledger_id)    REFERENCES ledger_master(ledger_id)      ON DELETE CASCADE,
    FOREIGN KEY (asset_id)     REFERENCES asset_master(asset_id),
    FOREIGN KEY (category_id)  REFERENCES ledger_category(category_id),
    FOREIGN KEY (rule_id)      REFERENCES fixed_expense_rule(rule_id)
);

CREATE TABLE regular_income_receipt (
    ledger_id        INT PRIMARY KEY,
    asset_id         BIGINT,
    category_id      INT,
    rule_id          INT,
    amount           BIGINT NOT NULL,
    transaction_date DATE NOT NULL,
    FOREIGN KEY (ledger_id)    REFERENCES ledger_master(ledger_id)      ON DELETE CASCADE,
    FOREIGN KEY (asset_id)     REFERENCES asset_master(asset_id),
    FOREIGN KEY (category_id)  REFERENCES ledger_category(category_id),
    FOREIGN KEY (rule_id)      REFERENCES regular_income_rule(rule_id)
);

CREATE TABLE variable_expense_receipt (
    ledger_id        INT PRIMARY KEY,
    asset_id         BIGINT,
    category_id      INT,
    rule_id          INT,
    amount           BIGINT,
    transaction_date DATE NOT NULL,
    status           VARCHAR(20) DEFAULT 'PENDING',
    FOREIGN KEY (ledger_id)    REFERENCES ledger_master(ledger_id)      ON DELETE CASCADE,
    FOREIGN KEY (asset_id)     REFERENCES asset_master(asset_id),
    FOREIGN KEY (category_id)  REFERENCES ledger_category(category_id),
    FOREIGN KEY (rule_id)      REFERENCES variable_expense_rule(rule_id)
);

-- FK 체크 복원
SET FOREIGN_KEY_CHECKS = 1;
