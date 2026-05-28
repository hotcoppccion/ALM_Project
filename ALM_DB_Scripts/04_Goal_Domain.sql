-- ═══════════════════════════════════════════════════════════════════════
--  04_Goal_Domain.sql  |  재무 목표 도메인 테이블 생성 (총 1개)
--
--  [기능 설명]
--    사용자가 설정한 재무 목표(예: "1억 모으기", "비상금 300만원")를 저장합니다.
--    목표 달성률(%)은 DB 에 저장하지 않고, Java GoalService 에서 실시간 계산합니다.
--    달성률 = (현재 자산 금액 ÷ target_amount) × 100
--
--  [asset_id 설계]
--    NULL 허용: 특정 계좌가 아닌 "전체 자산 합계" 기준 목표 시 NULL 저장
--    NOT NULL:  특정 계좌 잔액 기준 목표 시 해당 asset_id 저장
--
--  [ON DELETE SET NULL 의 의미]
--    목표와 연동된 자산이 삭제될 경우:
--      → 목표 행을 함께 삭제(CASCADE)하지 않고, asset_id 만 NULL 로 초기화
--    이렇게 하면 목표 자체는 살아있고, "전체 자산 합계" 기준 목표로 자동 전환됩니다.
--    사용자가 힘들게 설정한 목표 기록을 잃지 않기 위한 설계입니다.
-- ═══════════════════════════════════════════════════════════════════════

USE alm_db;

-- ── goal_table — 재무 목표 ────────────────────────────────────────────
-- goal_id       : AUTO_INCREMENT 기본키. 여러 목표를 등록할 수 있으므로 필요.
-- asset_id      : NULL 허용. 특정 자산 연동 시 asset_id 저장, 전체 합계 기준이면 NULL.
-- goal_name     : 목표 이름 (예: "비상금 목표", "해외여행 적금")
-- target_amount : 달성 목표 금액 (원 단위 정수, BIGINT)
-- end_date      : 목표 마감일. NULL = 기한 없음 (기한 없는 목표도 허용)
--
-- DATE 타입 : DATETIME 과 달리 날짜만 저장 (시간 정보 없음)
--   예: '2025-12-31' 형식으로 저장 및 조회됩니다.
CREATE TABLE goal_table (
    goal_id       INT AUTO_INCREMENT PRIMARY KEY,   -- 목표 고유 ID
    asset_id      INT,                               -- 연동 자산 ID (NULL = 전체 자산 기준)
    goal_name     VARCHAR(100) NOT NULL,             -- 목표명 (예: "비상금 300만원")
    target_amount BIGINT NOT NULL,                   -- 목표 금액 (원)
    end_date      DATE,                              -- 목표 마감일 (NULL = 기한 없음)

    -- ON DELETE SET NULL : 연동 자산 삭제 시 목표를 삭제하지 않고 asset_id 만 NULL 로 초기화
    FOREIGN KEY (asset_id) REFERENCES asset_master(asset_id) ON DELETE SET NULL
);

SELECT '✅ 04 목표 도메인 생성 완료 (1개 테이블: goal_table)' AS result;
