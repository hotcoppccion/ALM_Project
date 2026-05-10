-- 02_Asset_Domain.sql
-- 자산 마스터 및 각 자산 상세 테이블 생성

-- 1. 자산 마스터 (모든 자산의 부모)
CREATE TABLE asset_master (
    asset_id INT AUTO_INCREMENT PRIMARY KEY,
    type_code VARCHAR(20) NOT NULL, -- ACC(계좌), PHY(실물), RE(부동산), CASH(현금)
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 2. 은행 마스터
CREATE TABLE account_bank (
    bank_id INT AUTO_INCREMENT PRIMARY KEY,
    bank_name VARCHAR(50) NOT NULL
);

-- 3. 통장 종류 마스터
CREATE TABLE account_type (
    type_id INT AUTO_INCREMENT PRIMARY KEY,
    type_name VARCHAR(50) NOT NULL -- 예적금, 입출금, CMA 등
);

-- 4. 금융 계좌 (asset_master 상속)
CREATE TABLE account_table (
    asset_id INT PRIMARY KEY,
    bank_id INT,
    type_id INT,
    acc_number VARCHAR(50),
    balance DECIMAL(18, 2) DEFAULT 0,
    account_interest DECIMAL(5, 2),
    FOREIGN KEY (asset_id) REFERENCES asset_master(asset_id) ON DELETE CASCADE,
    FOREIGN KEY (bank_id) REFERENCES account_bank(bank_id),
    FOREIGN KEY (type_id) REFERENCES account_type(type_id)
);

-- 5. 실물 자산 (asset_master 상속)
CREATE TABLE physical_asset (
    asset_id INT PRIMARY KEY,
    item_name VARCHAR(100) NOT NULL,
    purchase_price DECIMAL(18, 2),
    current_value DECIMAL(18, 2),
    last_updated DATETIME,
    FOREIGN KEY (asset_id) REFERENCES asset_master(asset_id) ON DELETE CASCADE
);

-- 6. 부동산 (asset_master 상속)
CREATE TABLE real_estate (
    asset_id INT PRIMARY KEY,
    contract_type VARCHAR(20), -- 전세, 월세, 매매
    address VARCHAR(255),
    price DECIMAL(18, 2),
    FOREIGN KEY (asset_id) REFERENCES asset_master(asset_id) ON DELETE CASCADE
);

-- 7. 현금 자산 (asset_master 상속)
CREATE TABLE cash_asset (
    asset_id INT PRIMARY KEY,
    name VARCHAR(50),
    balance DECIMAL(18, 2) DEFAULT 0,
    FOREIGN KEY (asset_id) REFERENCES asset_master(asset_id) ON DELETE CASCADE
);