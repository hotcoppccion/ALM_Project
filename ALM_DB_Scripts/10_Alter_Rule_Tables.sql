-- ═══════════════════════════════════════════════════════════
--  10_Alter_Rule_Tables.sql
--  규칙 테이블(fixed_expense_rule / regular_income_rule / variable_expense_rule)에
--  name(규칙명)과 category_id(카테고리 FK) 컬럼을 추가하는 DDL 스크립트.
--
--  실행 전제: 03_Ledger_Domain.sql이 먼저 실행되어 해당 테이블이 존재해야 함.
--  실행 순서: 이 파일은 03 이후 최초 1회만 실행.
-- ═══════════════════════════════════════════════════════════

-- ──────────────────────────────────────────────────────────
--  1. fixed_expense_rule 컬럼 추가
--     name        : 지출 규칙 이름 (예: "월세", "인터넷 요금")
--     category_id : ledger_category 참조 FK (NULL 허용)
-- ──────────────────────────────────────────────────────────
ALTER TABLE fixed_expense_rule
  ADD COLUMN name VARCHAR(100) NOT NULL DEFAULT '미지정' AFTER rule_id,
  ADD COLUMN category_id INT NULL AFTER name,
  ADD CONSTRAINT fk_fer_cat FOREIGN KEY (category_id) REFERENCES ledger_category(category_id);

-- ──────────────────────────────────────────────────────────
--  2. regular_income_rule 컬럼 추가
--     name        : 수입 규칙 이름 (예: "월급", "용돈")
--     category_id : ledger_category 참조 FK (NULL 허용)
-- ──────────────────────────────────────────────────────────
ALTER TABLE regular_income_rule
  ADD COLUMN name VARCHAR(100) NOT NULL DEFAULT '미지정' AFTER rule_id,
  ADD COLUMN category_id INT NULL AFTER name,
  ADD CONSTRAINT fk_rir_cat FOREIGN KEY (category_id) REFERENCES ledger_category(category_id);

-- ──────────────────────────────────────────────────────────
--  3. variable_expense_rule 컬럼 추가
--     name        : 변동지출 규칙 이름 (예: "가스비", "전기세")
--     category_id : ledger_category 참조 FK (NULL 허용)
-- ──────────────────────────────────────────────────────────
ALTER TABLE variable_expense_rule
  ADD COLUMN name VARCHAR(100) NOT NULL DEFAULT '미지정' AFTER rule_id,
  ADD COLUMN category_id INT NULL AFTER name,
  ADD CONSTRAINT fk_ver_cat FOREIGN KEY (category_id) REFERENCES ledger_category(category_id);
