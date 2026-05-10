
-- ==========================================
-- 1) 가계부 마스터 (영수증 테이블들의 부모)
CREATE TABLE ledger_master (
    ledger_id INT AUTO_INCREMENT PRIMARY KEY,
    type_code VARCHAR(20) NOT NULL, -- GEN(일반), FIX_R(고정지출), REG_R(고정수입), VAR_R(변동지출)
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 2) 가계부 카테고리 마스터
CREATE TABLE ledger_category (
    category_id INT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(50) NOT NULL,
    is_default BOOLEAN DEFAULT FALSE
);

-- [스케줄러 규칙 테이블 - 상속 없음]
-- 3) 고정 지출 규칙
CREATE TABLE fixed_expense_rule (
    rule_id INT AUTO_INCREMENT PRIMARY KEY,
    amount DECIMAL(18, 2) NOT NULL,
    base_date DATE NOT NULL,
    p_value INT NOT NULL,
    p_unit VARCHAR(10) NOT NULL -- DAY, WEEK, MONTH, YEAR
);

-- 4) 고정 수입 규칙
CREATE TABLE regular_income_rule (
    rule_id INT AUTO_INCREMENT PRIMARY KEY,
    amount DECIMAL(18, 2) NOT NULL,
    base_date DATE NOT NULL,
    p_value INT NOT NULL,
    p_unit VARCHAR(10) NOT NULL
);

-- 5) 정기 변동 규칙
CREATE TABLE variable_expense_rule (
    rule_id INT AUTO_INCREMENT PRIMARY KEY,
    base_date DATE NOT NULL,
    p_value INT NOT NULL,
    p_unit VARCHAR(10) NOT NULL
);

-- [영수증 실행 테이블 - ledger_master 상속]
-- 6) 일반 지출입 (수동 입력 영수증)
CREATE TABLE general_ledger (
    ledger_id INT PRIMARY KEY,
    asset_id INT,
    category_id INT,
    amount DECIMAL(18, 2) NOT NULL,
    transaction_date DATE NOT NULL,
    FOREIGN KEY (ledger_id) REFERENCES ledger_master(ledger_id) ON DELETE CASCADE,
    FOREIGN KEY (asset_id) REFERENCES asset_master(asset_id),
    FOREIGN KEY (category_id) REFERENCES ledger_category(category_id)
);

-- 7) 고정 지출 영수증
CREATE TABLE fixed_expense_receipt (
    ledger_id INT PRIMARY KEY,
    asset_id INT,
    category_id INT,
    rule_id INT,
    amount DECIMAL(18, 2) NOT NULL,
    transaction_date DATE NOT NULL,
    FOREIGN KEY (ledger_id) REFERENCES ledger_master(ledger_id) ON DELETE CASCADE,
    FOREIGN KEY (asset_id) REFERENCES asset_master(asset_id),
    FOREIGN KEY (category_id) REFERENCES ledger_category(category_id),
    FOREIGN KEY (rule_id) REFERENCES fixed_expense_rule(rule_id)
);

-- 8) 고정 수입 영수증
CREATE TABLE regular_income_receipt (
    ledger_id INT PRIMARY KEY,
    asset_id INT,
    category_id INT,
    rule_id INT,
    amount DECIMAL(18, 2) NOT NULL,
    transaction_date DATE NOT NULL,
    FOREIGN KEY (ledger_id) REFERENCES ledger_master(ledger_id) ON DELETE CASCADE,
    FOREIGN KEY (asset_id) REFERENCES asset_master(asset_id),
    FOREIGN KEY (category_id) REFERENCES ledger_category(category_id),
    FOREIGN KEY (rule_id) REFERENCES regular_income_rule(rule_id)
);

-- 9) 변동 지출 영수증
CREATE TABLE variable_expense_receipt (
    ledger_id INT PRIMARY KEY,
    asset_id INT,
    category_id INT,
    rule_id INT,
    amount DECIMAL(18, 2),
    transaction_date DATE NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    FOREIGN KEY (ledger_id) REFERENCES ledger_master(ledger_id) ON DELETE CASCADE,
    FOREIGN KEY (asset_id) REFERENCES asset_master(asset_id),
    FOREIGN KEY (category_id) REFERENCES ledger_category(category_id),
    FOREIGN KEY (rule_id) REFERENCES variable_expense_rule(rule_id)
);