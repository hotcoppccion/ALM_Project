-- ═══════════════════════════════════════════════════════════════════════
--  00_Drop_All.sql  |  전체 테이블 삭제 (초기화/재설계 시 사용)
--
--  [사용 목적]
--    개발 중 스키마를 완전히 갈아엎고 싶을 때 이 파일만 실행하면
--    alm_db 안의 모든 테이블이 제거됩니다.
--    이후 01 ~ 08 파일을 순서대로 다시 실행하면 DB 재구성 완료.
--
--  [DROP 순서]
--    FOREIGN KEY 가 걸린 자식 테이블을 부모보다 먼저 DROP 해야 합니다.
--    FOREIGN_KEY_CHECKS = 0 으로 순서 제약을 일시 해제한 뒤 일괄 삭제합니다.
--    개발/테스트 환경 전용 패턴. 운영 DB 에서는 사용 주의.
--
--  [stock_master 미사용]
--    종목 정보(ticker_code, ticker_name)는 매수 시점에
--    invest_portfolio / invest_log 에 비정규화 직접 저장합니다.
-- ═══════════════════════════════════════════════════════════════════════

USE alm_db;

SET FOREIGN_KEY_CHECKS = 0;

-- 투자 도메인
DROP TABLE IF EXISTS invest_log;
DROP TABLE IF EXISTS invest_portfolio;
DROP TABLE IF EXISTS stock_master;

-- 목표 도메인
DROP TABLE IF EXISTS goal_table;

-- 가계부 도메인 (자식 → 부모 순)
DROP TABLE IF EXISTS general_ledger;
DROP TABLE IF EXISTS ledger_master;
DROP TABLE IF EXISTS ledger_category;

-- 자산 도메인 (자식 → 부모 순)
DROP TABLE IF EXISTS account_table;
DROP TABLE IF EXISTS physical_asset;
DROP TABLE IF EXISTS real_estate;
DROP TABLE IF EXISTS cash_asset;
DROP TABLE IF EXISTS account_bank;
DROP TABLE IF EXISTS account_type;
DROP TABLE IF EXISTS asset_master;

-- 보안 도메인
DROP TABLE IF EXISTS user_security;

SET FOREIGN_KEY_CHECKS = 1;

SELECT '✅ 00 전체 테이블 삭제 완료 — 이제 01~08 순서대로 실행하세요' AS result;
