package com.alm.service;

import com.alm.repository.LedgerRepository;
import com.alm.util.ConstraintException;

/**
 * [Service 계층 - 무결성 검증 전담]
 * 자산 삭제 전 하위 도메인(가계부, 목표, 투자 등)에서의 참조 여부를 검증하는 클래스.
 *
 * [Java 문법 개념] 단일 책임 원칙(SRP - Single Responsibility Principle):
 *   - 이 클래스는 오직 "삭제 가능 여부 판단"만 담당.
 *   - AssetService에서 이 클래스를 호출하여 검증 결과를 받음 (역할 분리).
 *   - 여러 도메인의 Repository를 이 클래스에서 통합 호출함으로써 의존 관계 집중화.
 *
 * [사용자 정의 예외 설계]
 *   - Java 표준 예외(IllegalStateException 등)를 사용하지 않고 ConstraintException을 정의.
 *   - 이유: 에러 원인에 맞는 명확한 메시지를 Controller까지 전달하기 위함.
 */
public class DeleteValidatorService {

    // 가계부 도메인 Repository: 특정 asset_id를 참조하는 가계부 내역이 있는지 확인에 사용
    // [Java 문법 개념] 필드 초기화: 선언과 동시에 new로 객체 생성 (생성자 없이도 초기화 가능)
    private final LedgerRepository ledgerRepository = new LedgerRepository();

    /**
     * 삭제하려는 자산이 다른 테이블에서 참조되고 있는지 검사.
     * 참조가 하나라도 있으면 ConstraintException을 throw하여 삭제 프로세스 중단.
     *
     * [Java 문법 개념] throws 키워드:
     *   - 메서드 시그니처에 throws ConstraintException 선언 = 이 메서드는 해당 예외를 던질 수 있음을 명시.
     *   - checked exception: 호출자(AssetService)가 try-catch로 처리하거나 자신도 throws로 선언해야 함.
     *   - ConstraintException이 Exception을 상속하므로 checked exception에 해당.
     *
     * @param assetId 삭제 요청된 자산의 전역 식별자
     * @throws ConstraintException 참조 데이터 존재 시 발생. 메시지에 구체적 사유 포함.
     */
    public void checkDependency(long assetId) throws ConstraintException {
        System.out.println("[LOG] 자산 [" + assetId + "] 참조 무결성 검증 시작");

        // ── 가계부 참조 검증 (구현 완료) ──────────────────────────────
        // LedgerRepository.existsByAssetId(): general_ledger 테이블에서 COUNT(*) 조회
        boolean hasLedgerDependency = ledgerRepository.existsByAssetId(assetId);
        if (hasLedgerDependency) {
            // [Java 문법 개념] throw new Exception("메시지"):
            //   - throw: 예외 객체를 생성하여 호출 스택으로 전파하는 키워드.
            //   - new ConstraintException(...): 생성자에 에러 메시지 문자열 전달.
            //   - 이 예외는 AssetService.requestDeleteAsset()의 catch에서 잡혀 Controller로 전달됨.
            throw new ConstraintException("해당 자산과 연결된 가계부 지출/수입 내역이 존재하여 삭제할 수 없습니다.");
        }

        // ── 목표 참조 검증 (향후 구현 예정) ──────────────────────────
        boolean hasGoalDependency = false;
        if (hasGoalDependency) {
            throw new ConstraintException("해당 자산이 특정 재무 목표의 산정 기준으로 연동되어 있어 삭제할 수 없습니다.");
        }

        // ── 투자 참조 검증 (향후 구현 예정) ──────────────────────────
        boolean hasInvestDependency = false;
        if (hasInvestDependency) {
            throw new ConstraintException("해당 계좌와 연동된 주식 매매 이력이 존재하여 삭제할 수 없습니다.");
        }

        System.out.println("[LOG] 자산 [" + assetId + "] 참조 없음 → 삭제 허용");
    }

    /**
     * 중요 데이터 삭제 전, 사용자가 입력한 확인 문자열을 검증.
     *
     * [Java 문법 개념] String.equals() vs == 연산자:
     *   - == 연산자: 두 참조 변수가 메모리 상 동일한 객체를 가리키는지 비교 (주소 비교).
     *   - .equals(): 두 String 객체의 문자 내용이 같은지 비교 (값 비교). String 비교 시 반드시 사용.
     *   - "삭제".equals(inputString): 상수를 앞에 배치하여, inputString이 null이어도 NPE 방지.
     *     ("삭제"는 절대 null이 아니므로 .equals() 호출 안전)
     *
     * @param inputString 사용자가 확인창에 입력한 문자열
     * @return "삭제"와 정확히 일치하면 true
     */
    public boolean verifyConfirmString(String inputString) {
        return "삭제".equals(inputString);
    }
}