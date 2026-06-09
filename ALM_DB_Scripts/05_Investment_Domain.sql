-- ═══════════════════════════════════════════════════════════════════════
--  05_Investment_Domain.sql  |  투자 포트폴리오 도메인 테이블 생성 (총 2개)
--
--  [stock_master 미사용 설계]
--    ticker_code / ticker_name 을 invest_portfolio / invest_log 에 직접(비정규화) 저장합니다.
--    이유:
--      1) KIS OpenAPI 실시간 조회: 종목 목록을 로컬 DB 에 미리 구축·동기화할 필요가 없습니다.
--      2) 거래 당시의 종목명 영구 보존: 사명 변경 이후에도 거래 기록은 원래 이름 그대로 남습니다.
--      3) JOIN 없이 한 테이블만으로 완결된 거래 정보를 얻습니다.
--
--  [두 테이블 역할 분리]
--    invest_portfolio : 현재 보유 종목 잔고 스냅샷.
--                       전량 매도 시 해당 행 삭제(청산).
--    invest_log       : 전체 매매 이력 (복기 목적 영구 보존).
--                       행을 삭제하지 않습니다.
--
--  [asset_id 연동]
--    asset_id → account_table (type_code = 'ACC', account_type.type_name = '증권위탁계좌')
--    매수(BUY) : Java InvestService 가 해당 계좌 balance 를 (qty × price) 만큼 차감합니다.
--    매도(SELL): Java InvestService 가 해당 계좌 balance 를 (qty × price) 만큼 증가합니다.
-- ═══════════════════════════════════════════════════════════════════════

USE alm_db;

-- ── 1. invest_portfolio — 현재 보유 종목 (잔고 스냅샷) ────────────────
--
-- [UNIQUE KEY uq_account_ticker]
--   동일 계좌에서 동일 종목을 두 행으로 중복 등록하는 것을 방지합니다.
--   다른 계좌(asset_id 가 다른 경우)에서 동일 종목 보유는 허용됩니다.
--
-- [purchase_price — BIGINT]
--   가중 평균 매입 단가. 원화 환산 고가 주식도 수용하기 위해 BIGINT 사용.
--   WAC 계산은 Java InvestRepository.buyStock() 에서 처리합니다.
--
-- [ON DELETE CASCADE]
--   계좌(asset_master) 삭제 시 해당 계좌의 포트폴리오 전체 자동 삭제.
--   계좌 없는 포트폴리오 행은 의미가 없기 때문입니다.
--   goal_table 의 ON DELETE SET NULL 과 대비됩니다.
CREATE TABLE invest_portfolio (
    invest_id      INT          AUTO_INCREMENT PRIMARY KEY,
    asset_id       INT          NOT NULL,          -- 연동 증권위탁계좌 (필수)
    ticker_code    VARCHAR(20)  NOT NULL,           -- 종목 코드 (국내 6자리 또는 해외 영문)
    ticker_name    VARCHAR(100) NOT NULL,           -- 종목명 (매수 시점 직접 저장)
    quantity       INT          NOT NULL DEFAULT 0,
    purchase_price BIGINT       NOT NULL DEFAULT 0, -- 가중 평균 매입 단가 (원)

    UNIQUE KEY uq_account_ticker (asset_id, ticker_code),
    FOREIGN KEY (asset_id) REFERENCES asset_master(asset_id) ON DELETE CASCADE
);

-- ── 2. invest_log — 매매 이력 ────────────────────────────────────────
--
-- [asset_id — NULL 허용 + ON DELETE SET NULL]
--   계좌(asset_master) 가 삭제되더라도 거래 이력 행은 보존되어야 합니다.
--   계좌 삭제 시 해당 행들의 asset_id 가 NULL 로 초기화됩니다.
--   → "어느 계좌의 거래인지 알 수 없음" 상태로 이력은 유지됩니다.
--   ※ invest_portfolio 의 ON DELETE CASCADE 와 반대 설계입니다.
--      포트폴리오는 계좌 없이 의미가 없지만,
--      이력은 계좌가 삭제된 이후에도 복기 용도로 계속 유효합니다.
--
-- [realized_profit]
--   실현 손익 = (매도 단가 − 평균 매입 단가) × 매도 수량.
--   매수(BUY) 시에는 0 으로 저장합니다.
--   계산은 Java InvestRepository.sellStock() 에서 처리합니다.
--
-- [reason_basis — TEXT]
--   매매 근거 / 복기 메모. 선택 입력이므로 NULL 허용.
CREATE TABLE invest_log (
    invest_log_id    INT          AUTO_INCREMENT PRIMARY KEY,
    asset_id         INT,                            -- 거래 계좌 (계좌 삭제 시 NULL)
    ticker_code      VARCHAR(20)  NOT NULL,
    ticker_name      VARCHAR(100) NOT NULL,
    transaction_type VARCHAR(4)   NOT NULL,          -- 'BUY' 또는 'SELL'
    quantity         INT          NOT NULL,
    price            BIGINT       NOT NULL,
    realized_profit  BIGINT       NOT NULL DEFAULT 0, -- 매도 시 실현 손익, 매수 시 0
    reason_basis     TEXT,                           -- 매매 근거 메모 (선택)
    trade_date       DATE         NOT NULL,

    -- 계좌 삭제 시 이력 보존 + asset_id NULL 초기화
    FOREIGN KEY (asset_id) REFERENCES asset_master(asset_id) ON DELETE SET NULL
);

SELECT '✅ 05 투자 도메인 생성 완료 (2개 테이블: invest_portfolio, invest_log)' AS result;
