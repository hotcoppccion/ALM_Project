-- ═══════════════════════════════════════════════════════════════════════
--  04_Goal_Domain.sql  |  재무 목표 도메인 테이블 생성 (총 1개)
--
--  [기능]
--    사용자가 설정한 재무 목표(예: "1억 모으기", "비상금 300만원")를 저장합니다.
--    목표 달성률(%)은 DB 에 저장하지 않고 Java GoalService 에서 실시간 계산합니다.
--
--  [asset_id 설계]
--    NULL 허용 : "전체 자산 합계" 기준 목표 → asset_id = NULL
--    NOT NULL  : 특정 계좌 잔액 기준 목표 → 해당 asset_id 저장
--
--  [ON DELETE SET NULL 설계 근거]
--    연동 자산이 삭제될 때 목표 행을 CASCADE 로 함께 삭제하지 않고,
--    asset_id 만 NULL 로 초기화합니다.
--    → 목표 자체는 유지되고 "전체 자산 합계" 기준 목표로 자동 전환됩니다.
--    사용자가 설정한 목표 기록을 잃지 않기 위한 설계입니다.
-- ═══════════════════════════════════════════════════════════════════════

USE alm_db;

CREATE TABLE goal_table (
    goal_id       INT AUTO_INCREMENT PRIMARY KEY,
    asset_id      INT,                           -- 연동 자산 (NULL = 전체 자산 기준)
    goal_name     VARCHAR(100) NOT NULL,
    target_amount BIGINT NOT NULL,
    end_date      DATE,                          -- NULL = 기한 없음

    -- 자산 삭제 시 목표는 보존, asset_id 만 NULL 로 초기화
    FOREIGN KEY (asset_id) REFERENCES asset_master(asset_id) ON DELETE SET NULL
);

SELECT '✅ 04 목표 도메인 생성 완료 (1개 테이블: goal_table)' AS result;
