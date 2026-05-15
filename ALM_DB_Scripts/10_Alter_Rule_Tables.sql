-- ═══════════════════════════════════════════════════════════
--  10_Alter_Rule_Tables.sql  (MySQL 8.0 호환)
--
--  규칙 테이블 3종에 name(규칙명), category_id(FK) 컬럼 추가.
--
--  ※ IF NOT EXISTS 는 MariaDB 전용 문법 → MySQL 8.0에서 1064 오류 발생
--     → 해당 구문 제거, 순수 MySQL 호환 문장으로 작성
--  ※ 실행 전: 좌측 스키마에서 alm_db 더블클릭 → 활성화(볼드체) 확인 후 실행
--  ※ 만약 일부 컬럼이 이미 존재해서 오류가 나면 그 줄만 건너뛰고 나머지 실행
-- ═══════════════════════════════════════════════════════════

USE alm_db;

-- ──────────────────────────────────────────────────────────
--  1. fixed_expense_rule
-- ──────────────────────────────────────────────────────────
ALTER TABLE fixed_expense_rule
  ADD COLUMN `name` VARCHAR(100) NOT NULL DEFAULT '미지정' AFTER rule_id;

ALTER TABLE fixed_expense_rule
  ADD COLUMN category_id INT NULL AFTER `name`;

ALTER TABLE fixed_expense_rule
  ADD CONSTRAINT fk_fer_cat FOREIGN KEY (category_id) REFERENCES ledger_category(category_id);

-- ──────────────────────────────────────────────────────────
--  2. regular_income_rule
-- ──────────────────────────────────────────────────────────
ALTER TABLE regular_income_rule
  ADD COLUMN `name` VARCHAR(100) NOT NULL DEFAULT '미지정' AFTER rule_id;

ALTER TABLE regular_income_rule
  ADD COLUMN category_id INT NULL AFTER `name`;

ALTER TABLE regular_income_rule
  ADD CONSTRAINT fk_rir_cat FOREIGN KEY (category_id) REFERENCES ledger_category(category_id);

-- ──────────────────────────────────────────────────────────
--  3. variable_expense_rule
-- ──────────────────────────────────────────────────────────
ALTER TABLE variable_expense_rule
  ADD COLUMN `name` VARCHAR(100) NOT NULL DEFAULT '미지정' AFTER rule_id;

ALTER TABLE variable_expense_rule
  ADD COLUMN category_id INT NULL AFTER `name`;

ALTER TABLE variable_expense_rule
  ADD CONSTRAINT fk_ver_cat FOREIGN KEY (category_id) REFERENCES ledger_category(category_id);
