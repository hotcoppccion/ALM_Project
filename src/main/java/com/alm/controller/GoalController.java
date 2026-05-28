package com.alm.controller;

import com.alm.service.GoalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/**
 * 재무 목표 REST API.
 *
 * [API 목록]
 *   GET    /api/goals          전체 목표 목록 (달성률·D-day 포함)
 *   POST   /api/goals          목표 등록
 *   DELETE /api/goals/{id}     목표 삭제
 *   GET    /api/goals/assets   목표 등록 모달 자산 드롭다운
 */
@RestController
@RequestMapping("/api/goals")
public class GoalController {

    private final GoalService goalService = new GoalService();

    @GetMapping
    public ResponseEntity<?> getGoals() {
        try {
            return ResponseEntity.ok(goalService.getGoalsWithProgress());
        } catch (Exception e) {
            return ResponseEntity.ok(List.of());
        }
    }

    @PostMapping
    public ResponseEntity<?> addGoal(@RequestBody Map<String, Object> payload) {
        try {
            goalService.saveGoal(payload);
            return ResponseEntity.ok(Map.of("message", "목표가 등록되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteGoal(@PathVariable int id) {
        try {
            goalService.deleteGoal(id);
            return ResponseEntity.ok(Map.of("message", "목표가 삭제되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/assets")
    public ResponseEntity<?> getAssetsForSelect() {
        try {
            return ResponseEntity.ok(goalService.getAssetsForSelect());
        } catch (Exception e) {
            return ResponseEntity.ok(List.of());
        }
    }
}
