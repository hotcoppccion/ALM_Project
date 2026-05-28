-- ═══════════════════════════════════════════════════════════════════════
--  00_Drop_All.sql  |  전체 테이블 삭제 (초기화/재설계 시 사용)
--
--  [사용 목적]
--    개발 중 스키마를 완전히 갈아엎고 싶을 때 이 파일만 실행하면
--    alm_db 안의 모든 테이블이 제거됩니다.
--    이후 01 ~ 08 파일을 순서대로 다시 실행하면 DB 재구성 완료.
--
--  [DROP 순서가 중요한 이유 — FOREIGN KEY 제약]
--    MySQL 에서 FOREIGN KEY(외래키)가 걸린 테이블은
--    "참조되는 테이블(부모)"을 먼저 지우면 오류가 납니다.
--    예) account_table 이 asset_master 를 참조 → account_table 먼저 DROP
--
--  [FOREIGN_KEY_CHECKS = 0 을 사용하는 이유]
--    DROP 순서를 일일이 맞추지 않아도 되도록
--    FK 제약 검사를 잠시 끄고 전부 삭제한 뒤 다시 켭니다.
--    ※ 개발/테스트 환경 전용 패턴. 운영 DB에서는 사용 주의.
--
--  ★ stock_master 테이블은 본 ALM 설계에서 사용하지 않습니다.
--    종목 정보(ticker_code, ticker_name)는 매수 시점에
--    invest_portfolio 와 invest_log 에 직접(비정규화) 저장합니다.
-- ═══════════════════════════════════════════════════════════════════════

USE alm_db;

-- ── FK 제약 검사 비활성화 ─────────────────────────────────────────────
-- SET : SQL 세션 변수를 설정하는 명령어
-- FOREIGN_KEY_CHECKS = 0 : 이 세션에서만 외래키 검사를 일시 중단
SET FOREIGN_KEY_CHECKS = 0;

-- ── 투자 도메인 (Investment) ──────────────────────────────────────────
-- invest_log       : 매매 이력 (asset_id → asset_master 참조)
-- invest_portfolio : 현재 보유 종목 (asset_id → asset_master 참조)
-- DROP TABLE IF EXISTS : 테이블이 없어도 오류 없이 넘어감 (IF EXISTS)
DROP TABLE IF EXISTS invest_log;
DROP TABLE IF EXISTS invest_portfolio;

-- ── 목표 도메인 (Goal) ───────────────────────────────────────────────
-- goal_table : 재무 목표 (asset_id → asset_master, NULL 허용)
DROP TABLE IF EXISTS goal_table;

-- ── 가계부 도메인 (Ledger) ───────────────────────────────────────────
-- general_ledger  : 일반 지출입 상세 (ledger_id, asset_id, category_id 모두 FK)
-- ledger_master   : 가계부 마스터 (모든 가계부 내역의 공통 부모)
-- ledger_category : 지출/수입 카테고리 목록 (general_ledger 에서 category_id FK 참조)
-- ※ 자식(general_ledger)을 먼저, 부모(ledger_master, ledger_category)를 나중에 삭제
DROP TABLE IF EXISTS general_ledger;
DROP TABLE IF EXISTS ledger_master;
DROP TABLE IF EXISTS ledger_category;

-- ── 자산 도메인 ─ 상세 테이블 먼저 삭제 ────────────────────────────
-- 아래 4개 테이블은 모두 asset_master.asset_id 를 PK 겸 FK 로 사용하는 자식 테이블
-- → asset_master 보다 먼저 삭제해야 함
DROP TABLE IF EXISTS account_table;
DROP TABLE IF EXISTS physical_asset;
DROP TABLE IF EXISTS real_estate;
DROP TABLE IF EXISTS cash_asset;

-- account_bank, account_type : account_table 이 bank_id / type_id 로 참조
-- → account_table 삭제 후에 이 두 테이블 삭제 가능
DROP TABLE IF EXISTS account_bank;
DROP TABLE IF EXISTS account_type;

-- asset_master : 가장 마지막에 삭제 (모든 자산 자식 테이블이 이 테이블을 참조)
DROP TABLE IF EXISTS asset_master;

-- ── 보안/인증 도메인 (Security) ──────────────────────────────────────
-- user_security : 마스터 비밀번호 해시 저장 (다른 테이블이 참조하지 않아 언제든 삭제 가능)
DROP TABLE IF EXISTS user_security;

-- ── FK 제약 검사 재활성화 ─────────────────────────────────────────────
-- 삭제 완료 후 반드시 1로 복원 → 이후 INSERT/UPDATE 시 무결성 검사 정상 동작
SET FOREIGN_KEY_CHECKS = 1;

SELECT '✅ 00 전체 테이블 삭제 완료 — 이제 01~08 순서대로 실행하세요' AS result;
