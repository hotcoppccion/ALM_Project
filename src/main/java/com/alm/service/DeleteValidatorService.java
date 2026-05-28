package com.alm.service;

import com.alm.repository.LedgerRepository;
import com.alm.util.ConstraintException;

/**
 * 자산 삭제 전 참조 무결성 검증 전담 서비스 (SRP).
 *
 * [설계 의도]
 *   AssetService 에서 삭제 가능 여부 판단 로직을 분리해 단일 책임 유지.
 *   도메인별 참조 검증을 한 곳에 모아 향후 도메인 추가 시 이 클래스만 수정.
 */
public class DeleteValidatorService {

    private final LedgerRepository ledgerRepository = new LedgerRepository();

    /**
     * 삭제 요청 자산의 하위 참조 검증.
     * 참조가 하나라도 있으면 ConstraintException throw → 삭제 프로세스 중단.
     *
     * [향후 구현 예정]
     *   목표 참조(GoalRepository.existsByAssetId), 투자 참조(InvestRepository.existsByAssetId) 추가.
     *
     * @throws ConstraintException 참조 데이터 존재 시 (메시지에 구체적 사유 포함)
     */
    public void checkDependency(long assetId) throws ConstraintException {
        if (ledgerRepository.existsByAssetId(assetId))
            throw new ConstraintException("해당 자산과 연결된 가계부 내역이 존재하여 삭제할 수 없습니다.");

        // TODO: GoalRepository.existsByAssetId(assetId) 구현 후 연동
        // TODO: InvestRepository.existsByAssetId(assetId) 구현 후 연동
    }

    /**
     * 사용자 확인 문자열 검증.
     * "삭제" 입력 시에만 삭제 진행 허용.
     */
    public boolean verifyConfirmString(String inputString) {
        return "삭제".equals(inputString);
    }
}
