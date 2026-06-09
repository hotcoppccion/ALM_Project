-- ═══════════════════════════════════════════════════════════════════════
--  08_Seed_LedgerCategories.sql  |  가계부 카테고리 기초(Seed) 데이터 삽입
--
--  [is_default = TRUE]
--    시스템 기본 제공 카테고리. Java LedgerService 에서 삭제를 차단합니다.
--
--  [지출/수입 구분]
--    이 테이블에는 구분 컬럼이 없습니다.
--    실제 금액의 부호(general_ledger.amount)로 구분합니다.
--    amount > 0 = 수입 / amount < 0 = 지출
--
--  category_id : 지출 1~12, 수입 13~18 (AUTO_INCREMENT 삽입 순)
-- ═══════════════════════════════════════════════════════════════════════

USE alm_db;

-- ── 지출(Expense) 카테고리 ────────────────────────────────────────────
INSERT INTO ledger_category (category_name, is_default) VALUES
('식비',          TRUE),  -- 1
('교통비',        TRUE),  -- 2
('주거/통신',     TRUE),  -- 3
('의료/건강',     TRUE),  -- 4
('문화/여가',     TRUE),  -- 5
('보험/세금',     TRUE),  -- 6
('생필품',        TRUE),  -- 7
('의류/미용',     TRUE),  -- 8
('교육/학습',     TRUE),  -- 9
('경조사/회비',   TRUE),  -- 10
('구독/멤버십',   TRUE),  -- 11
('반려동물',      TRUE);  -- 12

-- ── 수입(Income) 카테고리 ─────────────────────────────────────────────
INSERT INTO ledger_category (category_name, is_default) VALUES
('급여',                TRUE),  -- 13
('상여금',              TRUE),  -- 14
('금융수익(이자/배당)', TRUE),  -- 15
('용돈',                TRUE),  -- 16
('중고판매/기타수입',   TRUE),  -- 17
('투자수익',            TRUE);  -- 18

SELECT '✅ 08 가계부 카테고리 시드 데이터 삽입 완료 (지출 12개 + 수입 6개 = 총 18개)' AS result;
