package com.alm.service;

import com.alm.util.ConstraintException;

/**
 * [Business Logic Layer] 데이터의 안전한 삭제를 보장하기 위한 무결성 검증 전담 서비스 클래스입니다.
 * 자산 삭제 전 하위 도메인(가계부, 목표, 투자 등)의 참조 여부를 확인하고 사용자 확인 절차를 제어합니다.
 */
public class DeleteValidatorService {

    /**
     * 삭제하려는 자산 식별자가 다른 테이블에서 참조되고 있는지 검사합니다.
     * @param assetId 삭제 요청된 자산의 전역 식별자
     * @throws ConstraintException 참조 데이터가 존재하여 삭제가 불가할 경우 예외를 던짐
     */
    public void checkDependency(long assetId) throws ConstraintException {
        // [To-Do] 실제 연동 시 Repository들을 호출하여 참조 여부를 boolean으로 반환받음
        boolean hasLedgerDependency = false; // 가계부 영수증 테이블 참조 여부
        boolean hasGoalDependency = false;   // 목표 테이블 참조 여부
        boolean hasInvestDependency = false; // 투자 포트폴리오 참조 여부

        System.out.println("[LOG] 자산 식별자 [" + assetId + "] 참조 무결성 검증 시작");

        // 참조 데이터 발견 시 즉시 사용자 정의 예외를 발생시켜 프로세스 중단
        if (hasLedgerDependency) {
            throw new ConstraintException("해당 자산과 연결된 가계부 지출/수입 내역이 존재하여 삭제할 수 없습니다.");
        }

        if (hasGoalDependency) {
            throw new ConstraintException("해당 자산이 특정 재무 목표의 산정 기준으로 연동되어 있어 삭제할 수 없습니다.");
        }

        if (hasInvestDependency) {
            throw new ConstraintException("해당 계좌와 연동된 주식 매매 이력이 존재하여 삭제할 수 없습니다.");
        }
    }

    /**
     * 중요 데이터 삭제 전, 사용자가 안전 확인을 위해 입력한 문자열을 검증합니다.
     * @param inputString 사용자가 프롬프트 창 등에 입력한 문자열
     * @return "삭제" 문자열과 정확히 일치할 경우 true 반환
     */
    public boolean verifyConfirmString(String inputString) {
        // NullPointerException 방지를 위해 상수를 먼저 배치하여 equals 비교
        return "삭제".equals(inputString);
    }
}