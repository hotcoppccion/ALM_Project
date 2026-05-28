-- ═══════════════════════════════════════════════════════════════════════
--  08_Seed_LedgerCategories.sql  |  가계부 카테고리 기초(Seed) 데이터 삽입
--
--  [ledger_category 테이블의 역할]
--    가계부 내역 등록 시 "어떤 항목인지" 분류하는 드롭다운 목록.
--    지출 카테고리(식비, 교통비 등)와 수입 카테고리(급여, 이자 등)를
--    하나의 테이블로 통합 관리합니다.
--
--  [is_default 컬럼의 의미]
--    TRUE  (= 1) : 시스템 기본 제공 카테고리. 삭제 방지를 권장합니다.
--                  사용자가 UI 에서 "삭제" 버튼을 눌러도 Java Service 에서 차단합니다.
--    FALSE (= 0) : 사용자가 직접 추가한 커스텀 카테고리. 삭제 가능.
--    여기서는 모두 TRUE 로 삽입합니다 (모두 기본 제공).
--
--  [지출·수입 구분 방법]
--    이 테이블에는 지출/수입 구분 컬럼이 없습니다.
--    실제 금액의 부호(general_ledger.amount)로 구분합니다:
--      amount > 0 : 수입 (급여, 이자 등)
--      amount < 0 : 지출 (식비, 교통비 등)
--    따라서 UI 에서 "급여"를 선택한 후 양수 금액을 입력하면 수입,
--    실수로 식비에 양수를 입력해도 DB 구조상 막지 않습니다. (UX 검증은 Java 쪽에서)
--
--  [category_id 자동 부여 순서]
--    두 INSERT 문을 순서대로 실행하면 연속 번호가 부여됩니다.
--    지출 카테고리: 1~12, 수입 카테고리: 13~18
-- ═══════════════════════════════════════════════════════════════════════

USE alm_db;

-- ── 지출(Expense) 카테고리 ────────────────────────────────────────────
-- is_default = TRUE : 모두 시스템 기본 제공 카테고리
INSERT INTO ledger_category (category_name, is_default) VALUES
('식비',          TRUE),  -- category_id 1  : 음식, 배달, 카페
('교통비',        TRUE),  -- category_id 2  : 대중교통, 주유, 택시
('주거/통신',     TRUE),  -- category_id 3  : 월세, 관리비, 통신요금
('의료/건강',     TRUE),  -- category_id 4  : 병원, 약국, 헬스장
('문화/여가',     TRUE),  -- category_id 5  : 영화, 여행, 취미
('보험/세금',     TRUE),  -- category_id 6  : 보험료, 건강보험, 소득세
('생필품',        TRUE),  -- category_id 7  : 생활용품, 마트 쇼핑
('의류/미용',     TRUE),  -- category_id 8  : 옷, 신발, 미용실
('교육/학습',     TRUE),  -- category_id 9  : 수강료, 교재, 인강
('경조사/회비',   TRUE),  -- category_id 10 : 결혼식 축의금, 동아리비
('구독/멤버십',   TRUE),  -- category_id 11 : 넷플릭스, 유튜브 프리미엄
('반려동물',      TRUE);  -- category_id 12 : 사료, 동물병원

-- ── 수입(Income) 카테고리 ─────────────────────────────────────────────
INSERT INTO ledger_category (category_name, is_default) VALUES
('급여',                TRUE),  -- category_id 13 : 월급, 세후 급여
('상여금',              TRUE),  -- category_id 14 : 보너스, 성과급
('금융수익(이자/배당)', TRUE),  -- category_id 15 : 예금 이자, 배당금
('용돈',                TRUE),  -- category_id 16 : 부모님 용돈, 선물
('중고판매/기타수입',   TRUE),  -- category_id 17 : 당근마켓, 기타 수익
('투자수익',            TRUE);  -- category_id 18 : 주식 매도 차익 등

SELECT '✅ 08 가계부 카테고리 시드 데이터 삽입 완료 (지출 12개 + 수입 6개 = 총 18개)' AS result;
