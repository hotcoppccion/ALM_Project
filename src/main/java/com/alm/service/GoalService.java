package com.alm.service;

import com.alm.dto.GoalDTO;
import com.alm.repository.GoalRepository;
import com.alm.util.ParseUtil;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * 재무 목표 비즈니스 로직.
 *
 * [핵심 계산 3가지]
 *   ① current_amount : asset_id 지정 시 해당 자산 현재가치, NULL 이면 전체 자산 합계.
 *   ② achievement_rate : current / target × 100, 100% 초과분은 100 으로 고정.
 *   ③ days_remaining : 오늘 ~ end_date 일수. 기한 없는 목표는 Long.MAX_VALUE (센티널).
 */
public class GoalService {

    private final GoalRepository repo = new GoalRepository();

    // ── 조회 ─────────────────────────────────────────────────────────

    /**
     * 전체 목표 + 진행 현황 반환.
     *
     * [달성률 연산 순서]
     *   current * 100L / target : 먼저 100 을 곱해 정수 나눗셈 정밀도를 보존.
     *   (current / target * 100 으로 쓰면 소수 버림 후 곱셈이 되어 항상 0 이 나올 수 있음.)
     */
    public List<GoalDTO> getGoalsWithProgress() {
        List<GoalDTO> list = repo.findAllGoals();

        for (GoalDTO dto : list) {

            // ① 현재 금액
            long current;
            if (dto.getAsset_id() != null) {
                current = repo.getAssetCurrentValue(dto.getAsset_id());
            } else {
                current = repo.getTotalAssetValue();
                dto.setAsset_name("전체 자산 합계");
            }
            dto.setCurrent_amount(current);

            // ② 달성률 (0 ~ 100)
            long target = dto.getTarget_amount();
            int rate = (target > 0) ? (int) Math.min(100, current * 100L / target) : 0;
            dto.setAchievement_rate(rate);

            // ③ D-day (기한 없으면 Long.MAX_VALUE 센티널)
            if (dto.getEnd_date() != null && !dto.getEnd_date().isEmpty()) {
                LocalDate endDate = LocalDate.parse(dto.getEnd_date());
                dto.setDays_remaining(ChronoUnit.DAYS.between(LocalDate.now(), endDate));
            } else {
                dto.setDays_remaining(Long.MAX_VALUE);
            }
        }

        return list;
    }

    // ── 등록 ─────────────────────────────────────────────────────────

    /**
     * 목표 등록.
     *
     * [asset_id 처리]
     *   0 또는 미전송 시 null 저장 → 전체 자산 기준 목표로 해석.
     *   특정 자산 기준이면 해당 asset_id 저장.
     *
     * [end_date 처리]
     *   빈 문자열이면 null 저장 → 기한 없는 목표.
     *
     * @param payload {goal_name, target_amount, end_date(선택), asset_id(선택)}
     */
    public void saveGoal(Map<String, Object> payload) throws Exception {
        GoalDTO dto = new GoalDTO();

        String name = (String) payload.get("goal_name");
        if (name == null || name.trim().isEmpty()) throw new Exception("목표명을 입력하세요.");
        dto.setGoal_name(name.trim());

        long target = ParseUtil.parseLong(payload.get("target_amount"));
        if (target <= 0) throw new Exception("목표 금액을 올바르게 입력하세요.");
        dto.setTarget_amount(target);

        int assetId = ParseUtil.parseInt(payload.get("asset_id"), 0);
        dto.setAsset_id(assetId > 0 ? assetId : null);

        String endDate = (String) payload.getOrDefault("end_date", "");
        dto.setEnd_date(endDate.isEmpty() ? null : endDate);

        int id = repo.insertGoal(dto);
        if (id == -1) throw new Exception("목표 등록에 실패했습니다.");
    }

    // ── 삭제 ─────────────────────────────────────────────────────────

    /** @throws Exception 목표 미발견 또는 삭제 실패 시 */
    public void deleteGoal(int goalId) throws Exception {
        GoalDTO existing = repo.findGoalById(goalId);
        if (existing == null) throw new Exception("해당 목표를 찾을 수 없습니다.");
        if (!repo.deleteGoal(goalId)) throw new Exception("목표 삭제에 실패했습니다.");
    }

    // ── 자산 드롭다운 ─────────────────────────────────────────────────

    /** 목표 등록 모달용 자산 목록. */
    public List<Map<String, Object>> getAssetsForSelect() {
        return repo.findAllAssetsForSelect();
    }
}
