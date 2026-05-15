package com.alm.controller;

import com.alm.dto.DashboardSummaryDTO;
import com.alm.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * [Controller 계층 - 대시보드]
 * 메인 화면(main.html)에서 호출하는 대시보드 집계 데이터를 반환하는 컨트롤러.
 * 기획서 5.7절에 정의된 클래스.
 *
 * [엔드포인트]
 *   GET /api/dashboard/summary → 총 자산, 이달 수입/지출/순수지 반환
 *
 * [Java 문법 개념] @RestController:
 *   - @Controller + @ResponseBody의 합성 어노테이션.
 *   - 모든 메서드의 반환값이 JSON으로 자동 직렬화됨.
 *   - DashboardSummaryDTO → {"totalAsset": 0, "monthlyIncome": 0, ...} 형태로 변환.
 *
 * [Java 문법 개념] @RequestMapping("/api/dashboard"):
 *   - 클래스 레벨에서 공통 URL 접두사 지정.
 *   - 메서드의 @GetMapping과 결합: @GetMapping("/summary") → GET /api/dashboard/summary
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    // [Java 문법 개념] Eager Initialization:
    //   - 생성자 없이 필드 선언부에서 직접 new DashboardService() 실행.
    //   - final: DashboardService 인스턴스 참조의 재할당을 컴파일 시점에 차단.
    private final DashboardService dashboardService = new DashboardService();

    /**
     * [기획서 5.7절] 대시보드 요약 정보를 반환하는 엔드포인트.
     * GET /api/dashboard/summary
     *
     * [Java 문법 개념] ResponseEntity<DashboardSummaryDTO>:
     *   - ResponseEntity: HTTP 상태 코드(200, 404 등) + body를 함께 반환하는 Spring 래퍼.
     *   - <DashboardSummaryDTO>: 제네릭 타입 파라미터. body의 타입을 컴파일 타임에 명시.
     *   - ResponseEntity.ok(dto): HTTP 200 OK 상태 + dto를 body로 담아 반환하는 정적 팩토리 메서드.
     *
     * [현재 구현]
     *   - DashboardService.aggregateDashboardData()가 빈 DTO를 반환 (스텁).
     *   - 실제 집계 로직 완성 후 자동으로 올바른 데이터가 반환됨 (Controller 수정 불필요).
     *
     * @return DashboardSummaryDTO를 body에 담은 200 OK 응답
     */
    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryDTO> getDashboardSummary() {
        // dashboardService.aggregateDashboardData(): 총자산 + 이달 수입/지출 집계
        DashboardSummaryDTO dto = dashboardService.aggregateDashboardData();
        return ResponseEntity.ok(dto);
    }
}
