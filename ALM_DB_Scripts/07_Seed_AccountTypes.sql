-- ═══════════════════════════════════════════════════════════════════════
--  07_Seed_AccountTypes.sql  |  계좌 종류 기초(Seed) 데이터 삽입
--
--  ★★★ '증권위탁계좌' 문자열 수정 금지 ★★★
--    Java InvestRepository.findBrokerageAccounts() 에서 이 이름으로 계좌를 식별합니다.
--    수정 시 투자 포트폴리오 연동 계좌 조회가 동작하지 않습니다.
--    수정이 필요하면 Java 쿼리 문자열도 반드시 함께 변경해야 합니다.
--
--  ★★★ 'ISA(개인종합자산관리)' 문자열 수정 금지 ★★★
--    동일하게 Java InvestRepository.findBrokerageAccounts() WHERE 절에 포함되어 있습니다.
-- ═══════════════════════════════════════════════════════════════════════

USE alm_db;

INSERT INTO account_type (type_name) VALUES
('보통예금(입출금)'),       -- type_id 1
('정기예금'),               -- type_id 2
('정기적금'),               -- type_id 3
('자유적금'),               -- type_id 4
('CMA'),                    -- type_id 5
('ISA(개인종합자산관리)'),  -- type_id 6  ★ 투자 연동 대상
('주택청약저축'),           -- type_id 7
('외화예금'),               -- type_id 8
('증권위탁계좌'),           -- type_id 9  ★ 투자 연동 대상
('파킹통장'),               -- type_id 10
('마이너스통장'),           -- type_id 11
('대출계좌'),               -- type_id 12
('선불충전금(페이)'),       -- type_id 13
('포인트/캐시');             -- type_id 14

SELECT '✅ 07 계좌 종류 시드 데이터 삽입 완료' AS result;
