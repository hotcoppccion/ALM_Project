-- 09_Insert_Ledger_Category_Data.sql
-- 가계부 지출 및 수입 카테고리 기초 데이터 삽입

-- 1. 지출 카테고리 (is_default = 1)
INSERT INTO ledger_category (category_name, is_default) VALUES 
('식비', 1),
('교통비', 1),
('주거/통신', 1),
('의료/건강', 1),
('문화/여가', 1),
('보험/세금', 1),
('생필품', 1),
('의류/미용', 1),
('교육/학습', 1),
('경조사/회비', 1);

-- 2. 수입 카테고리 (is_default = 1)
INSERT INTO ledger_category (category_name, is_default) VALUES 
('급여', 1),
('상여금', 1),
('금융수익(이자/배당)', 1),
('용돈', 1),
('중고판매/기타수입', 1);