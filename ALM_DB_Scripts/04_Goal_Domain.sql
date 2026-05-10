-- 04_Goal_Domain.sql
-- 지능형 목표 관리 테이블 생성

CREATE TABLE goal_table (
    goal_id INT AUTO_INCREMENT PRIMARY KEY,
    asset_id INT, -- NULL 허용: 특정 계좌가 아닌 '전체 자산'을 목표로 할 경우 NULL로 둡니다.
    goal_name VARCHAR(100) NOT NULL,
    target_amount DECIMAL(18, 2) NOT NULL,
    end_date DATE,
    FOREIGN KEY (asset_id) REFERENCES asset_master(asset_id)
);