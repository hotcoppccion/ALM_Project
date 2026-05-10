-- 05_Investment_Domain.sql
-- 투자 종목, 포트폴리오, 매매 일지 테이블 생성

-- 1. 주식 종목 마스터
CREATE TABLE stock_master (
    stock_id INT AUTO_INCREMENT PRIMARY KEY,
    ticker_code VARCHAR(20) UNIQUE NOT NULL,
    ticker_name VARCHAR(100) NOT NULL,
    market_type VARCHAR(20) -- KOSPI, KOSDAQ, NASDAQ 등
);

-- 2. 투자 포트폴리오 (현재 보유 현황)
CREATE TABLE invest_portfolio (
    invest_id INT AUTO_INCREMENT PRIMARY KEY,
    asset_id INT,
    stock_id INT,
    quantity DECIMAL(18, 8) NOT NULL, -- 소수점 주식(미니스탁 등) 고려
    purchase_price DECIMAL(18, 2) NOT NULL,
    FOREIGN KEY (asset_id) REFERENCES asset_master(asset_id),
    FOREIGN KEY (stock_id) REFERENCES stock_master(stock_id)
);

-- 3. 투자 매매 일지 (히스토리)
CREATE TABLE invest_log (
    invest_log_id INT AUTO_INCREMENT PRIMARY KEY,
    asset_id INT,
    stock_id INT,
    transaction_type VARCHAR(10), -- BUY(매수), SELL(매도)
    amount DECIMAL(18, 8), -- 매매 수량
    price DECIMAL(18, 2),  -- 매매 단가
    realized_profit DECIMAL(18, 2), -- 실현 손익 (매도 시)
    reason_basis TEXT, -- 매매 근거 및 복기 내용
    transaction_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (asset_id) REFERENCES asset_master(asset_id),
    FOREIGN KEY (stock_id) REFERENCES stock_master(stock_id)
);