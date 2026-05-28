-- ═══════════════════════════════════════════════════════════════════════
--  05_Investment_Domain.sql  |  투자 포트폴리오 도메인 테이블 생성 (총 2개)
--
--  ★ 핵심 설계 결정 — stock_master 테이블 미사용 ★
--    [구(舊) 설계]  stock_master 라는 별도 종목 목록 테이블을 두고,
--                   invest_portfolio / invest_log 에서 stock_id(FK)로 참조
--    [신(新) 설계]  stock_master 테이블 자체를 제거하고,
--                   ticker_code / ticker_name 을
--                   invest_portfolio 와 invest_log 에 직접(비정규화) 저장
--
--    [왜 stock_master 를 없앴나?]
--      1) KIS OpenAPI 실시간 조회: 종목명·시세는 API 에서 즉시 가져오므로
--         로컬 DB 에 종목 목록을 미리 구축·동기화할 필요가 없습니다.
--      2) 비정규화(Denormalization) 의도: 거래 당시의 종목명을 영구 보존합니다.
--         나중에 종목명이 변경(예: 사명 변경)되더라도 거래 기록은 원래 이름 그대로 남습니다.
--      3) 단순성: JOIN 없이 한 테이블만 봐도 완결된 정보를 얻습니다.
--         trade_date, ticker_code, ticker_name, quantity, price 가 한 행에 전부 있음.
--
--  [두 테이블 역할 분리]
--    invest_portfolio : 현재 보유 종목 스냅샷 (잔고)
--                       → 수량이 0이 되면 행을 삭제(또는 0으로 유지)
--    invest_log       : 전체 매매 이력 (히스토리 / 복기용)
--                       → 절대 삭제하지 않는 영구 기록
--
--  [asset_id 연동]
--    asset_id → account_table (type_code = 'ACC', account_type.type_name = '증권위탁계좌')
--    매수(BUY)  : Java Service 에서 해당 계좌의 balance 를 (quantity × price) 만큼 차감
--    매도(SELL) : Java Service 에서 해당 계좌의 balance 를 (quantity × price) 만큼 증가
--    → "투자 등록 = 예수금 자동 반영" 트랜잭션 무결성 구현
-- ═══════════════════════════════════════════════════════════════════════

USE alm_db;

-- ── 1. invest_portfolio — 현재 보유 종목 (잔고 스냅샷) ─────────────────
--
-- [invest_id]
--   INT AUTO_INCREMENT PRIMARY KEY
--   AUTO_INCREMENT : 행 삽입 시 DB 가 자동으로 1씩 증가시켜 부여하는 정수 ID
--   PRIMARY KEY    : NOT NULL + UNIQUE 의 의미를 내포하는 제약 조건
--
-- [asset_id — NOT NULL]
--   어떤 증권위탁계좌에서 보유하는지 반드시 지정해야 합니다.
--   NULL 을 허용하지 않는 이유: 계좌 없는 포트폴리오는 의미가 없고,
--   매수 시 잔액 차감 대상 계좌를 반드시 알아야 하기 때문입니다.
--
-- [ticker_code — VARCHAR(20) NOT NULL]
--   종목 코드. 국내 6자리 숫자(예: '005930') 또는 해외 영문(예: 'AAPL')
--   VARCHAR(20) : 가변 길이 문자열. 최대 20자까지 저장하되 실제 길이만큼만 공간 사용.
--   KIS OpenAPI 에서 반환하는 코드 최대 길이 고려해 여유 있게 20으로 선언.
--
-- [ticker_name — VARCHAR(100) NOT NULL]
--   종목명 (예: '삼성전자', 'Apple Inc.')
--   stock_master 없이 직접 저장하므로, 매수 시점의 종목명이 영구 보존됩니다.
--
-- [quantity — INT NOT NULL DEFAULT 0]
--   보유 수량 (주). 주식은 정수 단위이므로 INT 사용.
--   DEFAULT 0 : 삽입 시 수량을 지정하지 않으면 자동으로 0 (정상 흐름에선 항상 지정).
--
-- [purchase_price — BIGINT NOT NULL DEFAULT 0]
--   가중 평균 매입 단가 (원 단위 정수).
--   BIGINT : 고가 주식(버크셔 해서웨이 등)도 원화 환산 시 매우 큰 수가 될 수 있으므로
--            INT(약 21억) 대신 BIGINT(약 9경) 사용.
--   [가중 평균 단가 계산 — Java Service 책임]
--     기존 보유분 + 추가 매수분을 합산해 단가를 재계산합니다.
--     공식: (기존수량 × 기존단가 + 신규수량 × 신규단가) ÷ (기존수량 + 신규수량)
--     DB 에는 결과값만 UPDATE 합니다. 계산 로직은 InvestService.java 에 위치.
--
-- [UNIQUE KEY uq_account_ticker (asset_id, ticker_code)]
--   복합 유니크 키: (계좌 ID + 종목 코드) 조합이 테이블 전체에서 유일해야 합니다.
--   의미: 같은 증권계좌에서 같은 종목을 두 개의 행으로 중복 등록할 수 없음.
--   단, 다른 계좌(asset_id 가 다른 경우)에서는 같은 ticker_code 를 보유 가능합니다.
--   예) asset_id=1(KB증권)에서 삼성전자 + asset_id=2(키움)에서 삼성전자 → 각각 별도 행 OK
--       asset_id=1에서 삼성전자 행 2개 → UNIQUE 위반 ✕
--
-- [FOREIGN KEY ... ON DELETE CASCADE]
--   account_table(=asset_master) 의 행이 삭제되면 → 이 테이블의 관련 포트폴리오 행도 자동 삭제
--   이유: 계좌가 없어졌는데 그 계좌의 보유 종목 기록이 남아 있으면 데이터 불일치 발생.
--         goal_table 의 ON DELETE SET NULL 과 다른 이유:
--         포트폴리오는 계좌가 없으면 아무 의미가 없지만(→ CASCADE),
--         목표는 계좌가 없어도 전체 자산 기준으로 계속 유효할 수 있음(→ SET NULL)
CREATE TABLE invest_portfolio (
    invest_id      INT          AUTO_INCREMENT PRIMARY KEY,  -- 포트폴리오 항목 고유 ID
    asset_id       INT          NOT NULL,                    -- 연동 증권위탁계좌 ID (account_table 참조)
    ticker_code    VARCHAR(20)  NOT NULL,                    -- 종목 코드 (국내 6자리 또는 해외 영문)
    ticker_name    VARCHAR(100) NOT NULL,                    -- 종목명 (매수 시점 이름 직접 저장)
    quantity       INT          NOT NULL DEFAULT 0,          -- 보유 수량 (주)
    purchase_price BIGINT       NOT NULL DEFAULT 0,          -- 가중 평균 매입 단가 (원)

    -- 복합 유니크 키: 동일 계좌에서 동일 종목 중복 보유 방지
    UNIQUE KEY uq_account_ticker (asset_id, ticker_code),

    -- 계좌 삭제 시 해당 계좌의 포트폴리오 전체 자동 삭제 (고아 레코드 방지)
    FOREIGN KEY (asset_id) REFERENCES asset_master(asset_id) ON DELETE CASCADE
);

-- ── 2. invest_log — 매매 이력 (전체 거래 히스토리) ───────────────────
--
-- [invest_log 와 invest_portfolio 의 관계]
--   invest_log 는 invest_portfolio 와 외래키 관계가 없습니다.
--   이유: 거래 이력(매도 포함)은 포트폴리오 행이 삭제된 후에도 영구 보존해야 하므로,
--         포트폴리오와의 FK 연결이 오히려 데이터 손실을 유발할 수 있습니다.
--   대신 asset_id + ticker_code 의 쌍으로 어떤 계좌의 어떤 종목인지 특정합니다.
--
-- [invest_log_id]
--   AUTO_INCREMENT PRIMARY KEY : 거래 건별 고유 ID 자동 부여
--
-- [asset_id — FK, ON DELETE 옵션 없음]
--   거래 당시의 계좌를 가리킵니다.
--   ON DELETE CASCADE 를 붙이지 않은 이유:
--     계좌가 삭제되더라도 그 계좌에서 발생한 거래 이력은 지우면 안 됩니다.
--     삭제된 계좌를 참조하는 행은 FK 위반이 생길 수 있으므로,
--     실제 운영에서는 계좌 삭제 전 로그를 확인하거나 별도 논리 삭제 처리 권장.
--     (본 학습용 프로젝트에서는 단순화를 위해 해당 처리를 생략합니다.)
--
-- [transaction_type — VARCHAR(4) NOT NULL]
--   'BUY'  : 매수 (주식 구매 → 계좌 balance 차감)
--   'SELL' : 매도 (주식 판매 → 계좌 balance 증가)
--   VARCHAR(4) : 'SELL' 이 4글자이므로 최소 필요 크기
--   CHECK 제약 조건을 추가할 수도 있으나, Java Service 에서 유효성 검증으로 대체합니다.
--
-- [realized_profit — BIGINT NOT NULL DEFAULT 0]
--   실현 손익 (원 단위 정수). 매도 시에만 의미 있는 값.
--   매수(BUY) 시에는 0 으로 저장합니다.
--   [실현 손익 계산 — Java Service 책임]
--     매도 단가 × 매도 수량 − 매입 단가 × 매도 수량
--     = (매도 단가 − 매입 단가) × 매도 수량
--     양수 = 이익, 음수 = 손실
--
-- [reason_basis — TEXT]
--   매매 근거 / 복기 메모. 선택 입력이므로 NULL 허용.
--   TEXT : VARCHAR 와 달리 최대 65,535바이트까지 저장 가능한 대용량 문자열 타입.
--          긴 분석 노트도 저장할 수 있도록 TEXT 사용.
--
-- [trade_date — DATE NOT NULL]
--   거래 날짜. DATE 타입은 날짜만 저장 (시간 정보 없음).
--   형식: '2025-12-31'
--   DATETIME 과 달리 시간 없이 날짜만 필요한 경우에 DATE 사용.
CREATE TABLE invest_log (
    invest_log_id    INT          AUTO_INCREMENT PRIMARY KEY,  -- 거래 이력 고유 ID
    asset_id         INT          NOT NULL,                    -- 거래 증권계좌 ID
    ticker_code      VARCHAR(20)  NOT NULL,                    -- 종목 코드
    ticker_name      VARCHAR(100) NOT NULL,                    -- 종목명 (거래 당시 이름 직접 저장)
    transaction_type VARCHAR(4)   NOT NULL,                    -- 거래 유형: 'BUY' 또는 'SELL'
    quantity         INT          NOT NULL,                    -- 거래 수량 (주)
    price            BIGINT       NOT NULL,                    -- 거래 단가 (원)
    realized_profit  BIGINT       NOT NULL DEFAULT 0,          -- 실현 손익 (매도 시 계산값, 매수 시 0)
    reason_basis     TEXT,                                     -- 매매 근거 / 복기 메모 (선택)
    trade_date       DATE         NOT NULL,                    -- 거래 날짜 (예: 2025-06-15)

    -- 계좌 참조 (CASCADE 없음 → 계좌 삭제 후에도 거래 이력 보존 의도)
    FOREIGN KEY (asset_id) REFERENCES asset_master(asset_id)
);

SELECT '✅ 05 투자 도메인 생성 완료 (2개 테이블: invest_portfolio, invest_log) — stock_master 미사용' AS result;
