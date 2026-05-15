-- ═══════════════════════════════════════════════════════════
--  10_Alter_Rule_Tables.sql
--  규칙 테이블(fixed_expense_rule / regular_income_rule / variable_expense_rule)에
--  name(규칙명)과 category_id(카테고리 FK) 컬럼을 추가하는 DDL 스크립트.
--
--  실행 전제: 03_Ledger_Domain.sql이 먼저 실행되어 해당 테이블이 존재해야 함.
--  실행 순서: 이 파일은 03 이후 최초 1회만 실행.
--
--  ※ IF NOT EXISTS 사용: 재실행 시 "Column already exists" 오류 방지 (MySQL 8.0+)
--  ※ name 백틱(`) 처리: MySQL 예약어 충돌 방지
--  ※ FK 제약조건은 컬럼 추가와 분리: 멱등성(idempotent) 보장
-- ═══════════════════════════════════════════════════════════

-- ──────────────────────────────────────────────────────────
--  1. fixed_expense_rule 컬럼 추가
-- ──────────────────────────────────────────────────────────
ALTER TABLE fixed_expense_rule
  ADD COLUMN IF NOT EXISTS `name` VARCHAR(100) NOT NULL DEFAULT '미지정' AFTER rule_id,
  ADD COLUMN IF NOT EXISTS category_id INT NULL AFTER `name`;

-- FK: 이미 존재하면 DROP 후 재생성 (중복 오류 방지)
ALTER TABLE fixed_expense_rule
  DROP FOREIGN KEY IF EXISTS fk_fer_cat;
ALTER TABLE fixed_expense_rule
  ADD CONSTRAINT fk_fer_cat FOREIGN KEY (category_id) REFERENCES ledger_category(category_id);

-- ──────────────────────────────────────────────────────────
--  2. regular_income_rule 컬럼 추가
-- ──────────────────────────────────────────────────────────
ALTER TABLE regular_income_rule
  ADD COLUMN IF NOT EXISTS `name` VARCHAR(100) NOT NULL DEFAULT '미지정' AFTER rule_id,
  ADD COLUMN IF NOT EXISTS category_id INT NULL AFTER `name`;

ALTER TABLE regular_income_rule
  DROP FOREIGN KEY IF EXISTS fk_rir_cat;
ALTER TABLE regular_income_rule
  ADD CONSTRAINT fk_rir_cat FOREIGN KEY (category_id) REFERENCES ledger_category(category_id);

-- ──────────────────────────────────────────────────────────
--  3. variable_expense_rule 컬럼 추가
-- ──────────────────────────────────────────────────────────
ALTER TABLE variable_expense_rule
  ADD COLUMN IF NOT EXISTS `name` VARCHAR(100) NOT NULL DEFAULT '미지정' AFTER rule_id,
  ADD COLUMN IF NOT EXISTS category_id INT NULL AFTER `name`;

ALTER TABLE variable_expense_rule
  DROP FOREIGN KEY IF EXISTS fk_ver_cat;
ALTER TABLE variable_expense_rule
  ADD CONSTRAINT fk_ver_cat FOREIGN KEY (category_id) REFERENCES ledger_category(category_id);
