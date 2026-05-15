package com.alm.controller;

import com.alm.dto.GeneralLedgerDTO;
import com.alm.service.LedgerService;
import com.alm.service.ScheduledLedgerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/**
 * [Controller 계층 - REST API]
 * 가계부 도메인의 HTTP 요청을 수신하고 응답을 반환하는 최전방 계층.
 *
 * [Java 문법 개념] Spring MVC 어노테이션:
 *
 * @RestController:
 *   - @Controller + @ResponseBody의 합성 어노테이션.
 *   - @Controller: 이 클래스가 Spring의 웹 요청 처리 컴포넌트임을 선언. Spring이 Bean으로 관리.
 *   - @ResponseBody: 메서드 반환값을 HTTP 응답 body에 JSON으로 직렬화하도록 지시.
 *   - Jackson 라이브러리가 Java 객체(DTO, Map, List)를 JSON 문자열로 자동 변환.
 *
 * @RequestMapping("/api/ledger"):
 *   - 이 클래스의 모든 메서드에 공통 URL 접두사("/api/ledger")를 적용.
 *   - 각 메서드의 @GetMapping, @PostMapping 등은 이 접두사 뒤에 붙는 경로를 추가로 지정.
 */
@RestController
@RequestMapping("/api/ledger")
public class LedgerController {

    // [Java 문법 개념] 필드 직접 주입 (Manual Dependency Injection):
    //   - Spring Boot에서 권장하는 방식은 생성자 주입 (@Autowired 생성자 또는 @RequiredArgsConstructor).
    //   - 이 프로젝트는 Spring Bean 주입을 사용하지 않고 직접 new로 인스턴스화.
    //   - 이유: 순수 JDBC 설계로 Spring Bean 컨테이너 의존성을 최소화하는 학습 목적.
    private final LedgerService ledgerService = new LedgerService();
    private final ScheduledLedgerService scheduledService = new ScheduledLedgerService();

    // ── 조회 ────────────────────────────────────────────────────────

    /**
     * 전체 가계부 내역 목록 반환.
     * GET /api/ledger/list
     *
     * [Java 문법 개념] ResponseEntity<T>:
     *   - HTTP 응답(상태 코드 + 헤더 + body)을 제어하는 Spring 클래스.
     *   - ResponseEntity.ok(body): 상태 코드 200 OK + body를 JSON으로 응답.
     *   - 제네릭 타입 T: body에 담을 데이터의 타입을 컴파일 시 지정. 타입 안전성 보장.
     *   - List<GeneralLedgerDTO>: GeneralLedgerDTO 객체들의 순서 있는 컬렉션. JSON 배열([ ])로 직렬화됨.
     */
    /**
     * [기획서 5.3절] 가계부 상세 내역 목록 반환. 기획서 메서드명: getLedgerDetail().
     * GET /api/ledger/list
     *
     * [네이밍 의도]
     *   - getLedgerDetail: 단순 목록(List)이지만 각 행에 category_name, asset_name 등
     *     JOIN으로 조합된 상세 정보가 포함되어 있음을 메서드명으로 표현.
     *   - URL(/api/ledger/list)은 변경 없음. Java 메서드명만 기획서와 일치시킴.
     */
    @GetMapping("/list")
    public ResponseEntity<List<GeneralLedgerDTO>> getLedgerDetail() {
        return ResponseEntity.ok(ledgerService.getAllGeneralLedger());
    }

    /**
     * 이달의 수입/지출/순수지 요약 반환.
     * GET /api/ledger/summary
     * 응답 예: {"totalIncome": 500000, "totalExpense": 120000, "net": 380000}
     */
    @GetMapping("/summary")
    public ResponseEntity<?> getMonthlySummary() {
        // <?> (와일드카드 제네릭): 반환 타입이 Map<String, Long>이지만, 에러 시 다른 타입도 반환할 수 있어 <?> 사용.
        return ResponseEntity.ok(ledgerService.getMonthlySummary());
    }

    /**
     * 카테고리 드롭다운 목록 반환.
     * GET /api/ledger/categories
     * 응답 예: [{"category_id": 1, "category_name": "식비"}, ...]
     */
    @GetMapping("/categories")
    public ResponseEntity<?> getCategories() {
        return ResponseEntity.ok(ledgerService.getCategories());
    }

    /**
     * 가계부 연동 가능한 자산(ACC + CSH) 목록 반환.
     * GET /api/ledger/liquid-assets
     * 응답 예: [{"asset_id": 1, "display_name": "카카오뱅크 (4567)", "type_code": "ACC"}, ...]
     */
    @GetMapping("/liquid-assets")
    public ResponseEntity<?> getLiquidAssets() {
        return ResponseEntity.ok(ledgerService.getLiquidAssets());
    }

    // ── 저장 ────────────────────────────────────────────────────────

    /**
     * 새 가계부 내역 등록 + 연동 자산 잔액 즉시 반영.
     * POST /api/ledger
     *
     * [Java 문법 개념] @PostMapping:
     *   - HTTP POST 메서드로 오는 요청을 이 메서드에 매핑.
     *   - POST: 서버에 새 리소스를 생성하는 HTTP 메서드. 멱등성(idempotent) 없음.
     *
     * @RequestBody Map<String, Object> payload:
     *   - HTTP 요청 body의 JSON 문자열을 Java Map으로 역직렬화.
     *   - Jackson이 자동 처리. 예: {"amount": 50000, "direction": "OUT", ...}
     *   - Map<String, Object>: 키는 String, 값은 타입 불명확(숫자/문자 혼재)이므로 Object.
     */
    @PostMapping
    public ResponseEntity<?> saveLedger(@RequestBody Map<String, Object> payload) {
        try {
            ledgerService.processGeneralLedger(payload);
            return ResponseEntity.ok(Map.of("message", "내역이 성공적으로 등록되었습니다."));
        } catch (Exception e) {
            // [Java 문법 개념] Exception 계층:
            //   - Throwable → Exception → RuntimeException (unchecked)
            //                           → IOException, SQLException 등 (checked)
            //   - catch (Exception e): 모든 checked/unchecked exception을 포괄하는 최상위 처리.
            //   - e.getMessage(): 예외 객체의 설명 문자열 반환. Service에서 throw new Exception("...")으로 설정.
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ── 삭제 ────────────────────────────────────────────────────────

    /**
     * 가계부 내역 삭제 + 자산 잔액 복원.
     * DELETE /api/ledger/{ledgerId}
     *
     * [Java 문법 개념] @DeleteMapping + @PathVariable:
     *   - @DeleteMapping("/{ledgerId}"): URL 경로에서 {} 중괄호로 감싼 부분이 경로 변수(Path Variable).
     *   - @PathVariable int ledgerId: URL의 {ledgerId} 값을 int 타입으로 바인딩.
     *     예: DELETE /api/ledger/5 → ledgerId = 5
     *   - HTTP DELETE: 서버의 리소스를 삭제하는 메서드. 멱등성 있음 (같은 요청 반복해도 결과 동일).
     */
    @DeleteMapping("/{ledgerId}")
    public ResponseEntity<?> deleteLedger(@PathVariable int ledgerId) {
        try {
            ledgerService.deleteGeneralLedger(ledgerId);
            return ResponseEntity.ok(Map.of("message", "내역이 삭제되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ══════════════════════════════════════════════════════════
    //  고정지출 규칙 (fixed_expense_rule)
    // ══════════════════════════════════════════════════════════

    /**
     * 고정지출 규칙 전체 목록 반환.
     * GET /api/ledger/fixed-rules
     */
    @GetMapping("/fixed-rules")
    public ResponseEntity<?> getFixedRules() {
        return ResponseEntity.ok(scheduledService.getFixedRules());
    }

    /**
     * 고정지출 규칙 등록.
     * POST /api/ledger/fixed-rules
     * Body: {name, category_id, amount, base_date, p_value, p_unit}
     */
    @PostMapping("/fixed-rules")
    public ResponseEntity<?> addFixedRule(@RequestBody Map<String, Object> payload) {
        try {
            scheduledService.saveFixedRule(payload);
            return ResponseEntity.ok(Map.of("message", "고정지출 규칙이 등록되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 고정지출 규칙 삭제.
     * DELETE /api/ledger/fixed-rules/{id}
     */
    @DeleteMapping("/fixed-rules/{id}")
    public ResponseEntity<?> deleteFixedRule(@PathVariable int id) {
        try {
            scheduledService.deleteFixedRule(id);
            return ResponseEntity.ok(Map.of("message", "고정지출 규칙이 삭제되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 고정지출 규칙 실행: 영수증 생성 + 자산 잔액 차감.
     * POST /api/ledger/fixed-rules/{id}/execute
     * Body: {asset_id, transaction_date}
     */
    @PostMapping("/fixed-rules/{id}/execute")
    public ResponseEntity<?> executeFixed(@PathVariable int id, @RequestBody Map<String, Object> payload) {
        try {
            scheduledService.executeFixedRule(id, payload);
            return ResponseEntity.ok(Map.of("message", "고정지출이 실행되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────
    //  고정지출 영수증 (fixed_expense_receipt)
    // ─────────────────────────────────────────────────────────

    /**
     * 고정지출 영수증 전체 목록 반환.
     * GET /api/ledger/fixed-receipts
     */
    @GetMapping("/fixed-receipts")
    public ResponseEntity<?> getFixedReceipts() {
        return ResponseEntity.ok(scheduledService.getFixedReceipts());
    }

    /**
     * 고정지출 영수증 삭제 + 자산 잔액 복원.
     * DELETE /api/ledger/fixed-receipts/{id}
     */
    @DeleteMapping("/fixed-receipts/{id}")
    public ResponseEntity<?> deleteFixedReceipt(@PathVariable int id) {
        try {
            scheduledService.deleteFixedReceipt(id);
            return ResponseEntity.ok(Map.of("message", "고정지출 내역이 삭제되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ══════════════════════════════════════════════════════════
    //  정기수입 규칙 (regular_income_rule)
    // ══════════════════════════════════════════════════════════

    /**
     * 정기수입 규칙 전체 목록 반환.
     * GET /api/ledger/income-rules
     */
    @GetMapping("/income-rules")
    public ResponseEntity<?> getIncomeRules() {
        return ResponseEntity.ok(scheduledService.getIncomeRules());
    }

    /**
     * 정기수입 규칙 등록.
     * POST /api/ledger/income-rules
     * Body: {name, category_id, amount, base_date, p_value, p_unit}
     */
    @PostMapping("/income-rules")
    public ResponseEntity<?> addIncomeRule(@RequestBody Map<String, Object> payload) {
        try {
            scheduledService.saveIncomeRule(payload);
            return ResponseEntity.ok(Map.of("message", "정기수입 규칙이 등록되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 정기수입 규칙 삭제.
     * DELETE /api/ledger/income-rules/{id}
     */
    @DeleteMapping("/income-rules/{id}")
    public ResponseEntity<?> deleteIncomeRule(@PathVariable int id) {
        try {
            scheduledService.deleteIncomeRule(id);
            return ResponseEntity.ok(Map.of("message", "정기수입 규칙이 삭제되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 정기수입 규칙 실행: 영수증 생성 + 자산 잔액 증가.
     * POST /api/ledger/income-rules/{id}/execute
     * Body: {asset_id, transaction_date}
     */
    @PostMapping("/income-rules/{id}/execute")
    public ResponseEntity<?> executeIncome(@PathVariable int id, @RequestBody Map<String, Object> payload) {
        try {
            scheduledService.executeIncomeRule(id, payload);
            return ResponseEntity.ok(Map.of("message", "정기수입이 실행되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────
    //  정기수입 영수증 (regular_income_receipt)
    // ─────────────────────────────────────────────────────────

    /**
     * 정기수입 영수증 전체 목록 반환.
     * GET /api/ledger/income-receipts
     */
    @GetMapping("/income-receipts")
    public ResponseEntity<?> getIncomeReceipts() {
        return ResponseEntity.ok(scheduledService.getIncomeReceipts());
    }

    /**
     * 정기수입 영수증 삭제 + 자산 잔액 복원.
     * DELETE /api/ledger/income-receipts/{id}
     */
    @DeleteMapping("/income-receipts/{id}")
    public ResponseEntity<?> deleteIncomeReceipt(@PathVariable int id) {
        try {
            scheduledService.deleteIncomeReceipt(id);
            return ResponseEntity.ok(Map.of("message", "정기수입 내역이 삭제되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ══════════════════════════════════════════════════════════
    //  변동지출 규칙 (variable_expense_rule)
    // ══════════════════════════════════════════════════════════

    /**
     * 변동지출 규칙 전체 목록 반환.
     * GET /api/ledger/variable-rules
     */
    @GetMapping("/variable-rules")
    public ResponseEntity<?> getVariableRules() {
        return ResponseEntity.ok(scheduledService.getVariableRules());
    }

    /**
     * 변동지출 규칙 등록.
     * POST /api/ledger/variable-rules
     * Body: {name, category_id, base_date, p_value, p_unit}  (amount 없음)
     */
    @PostMapping("/variable-rules")
    public ResponseEntity<?> addVariableRule(@RequestBody Map<String, Object> payload) {
        try {
            scheduledService.saveVariableRule(payload);
            return ResponseEntity.ok(Map.of("message", "변동지출 규칙이 등록되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 변동지출 규칙 삭제.
     * DELETE /api/ledger/variable-rules/{id}
     */
    @DeleteMapping("/variable-rules/{id}")
    public ResponseEntity<?> deleteVariableRule(@PathVariable int id) {
        try {
            scheduledService.deleteVariableRule(id);
            return ResponseEntity.ok(Map.of("message", "변동지출 규칙이 삭제되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 변동지출 규칙 발동: PENDING 상태 영수증 생성 (금액 미확정).
     * POST /api/ledger/variable-rules/{id}/trigger
     * Body: {asset_id, transaction_date}
     */
    @PostMapping("/variable-rules/{id}/trigger")
    public ResponseEntity<?> triggerVariable(@PathVariable int id, @RequestBody Map<String, Object> payload) {
        try {
            scheduledService.triggerVariableRule(id, payload);
            return ResponseEntity.ok(Map.of("message", "변동지출이 발생했습니다. 금액을 확정해 주세요."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────
    //  변동지출 영수증 (variable_expense_receipt)
    // ─────────────────────────────────────────────────────────

    /**
     * 변동지출 영수증 전체 목록 반환.
     * GET /api/ledger/variable-receipts
     */
    @GetMapping("/variable-receipts")
    public ResponseEntity<?> getVariableReceipts() {
        return ResponseEntity.ok(scheduledService.getVariableReceipts());
    }

    /**
     * 변동지출 영수증 확정: PENDING → CONFIRMED, 실제 금액 입력 + 자산 잔액 차감.
     * PATCH /api/ledger/variable-receipts/{id}/confirm
     * Body: {amount}  (사용자가 입력한 실제 금액, 양수)
     */
    @PatchMapping("/variable-receipts/{id}/confirm")
    public ResponseEntity<?> confirmVariable(@PathVariable int id, @RequestBody Map<String, Object> payload) {
        try {
            long amount = 0L;
            Object amtObj = payload.get("amount");
            if (amtObj != null) {
                try { amount = Long.parseLong(amtObj.toString().trim()); }
                catch (NumberFormatException ignored) {}
            }
            if (amount <= 0) return ResponseEntity.badRequest().body(Map.of("message", "금액을 올바르게 입력하세요."));
            scheduledService.confirmVariableReceipt(id, amount);
            return ResponseEntity.ok(Map.of("message", "변동지출이 확정되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 변동지출 영수증 삭제 + 자산 잔액 복원 (CONFIRMED인 경우).
     * DELETE /api/ledger/variable-receipts/{id}
     */
    @DeleteMapping("/variable-receipts/{id}")
    public ResponseEntity<?> deleteVariableReceipt(@PathVariable int id) {
        try {
            scheduledService.deleteVariableReceipt(id);
            return ResponseEntity.ok(Map.of("message", "변동지출 내역이 삭제되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
