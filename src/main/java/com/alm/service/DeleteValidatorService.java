package com.alm.service;

import com.alm.repository.GoalRepository;
import com.alm.repository.InvestRepository;
import com.alm.repository.LedgerRepository;
import com.alm.util.ConstraintException;

/**
 * 자산 삭제 전 타 도메인 참조 여부를 검증하는 서비스.
 * 새 도메인이 추가되면 checkDependency() 에만 검증 로직을 추가한다.
 */
public class DeleteValidatorService {

    private final LedgerRepository ledgerRepository = new LedgerRepository();
    private final GoalRepository   goalRepository   = new GoalRepository();
    private final InvestRepository investRepository = new InvestRepository();

    /** @throws ConstraintException 참조 데이터가 존재할 경우 */
    public void checkDependency(long assetId) throws ConstraintException {
        if (ledgerRepository.existsByAssetId(assetId))
            throw new ConstraintException("해당 자산과 연결된 가계부 내역이 존재하여 삭제할 수 없습니다.");
        if (goalRepository.existsByAssetId(assetId))
            throw new ConstraintException("해당 자산과 연결된 재무 목표가 존재하여 삭제할 수 없습니다.");
        if (investRepository.existsByAssetId(assetId))
            throw new ConstraintException("해당 계좌에 보유 중인 투자 종목이 존재하여 삭제할 수 없습니다.");
    }

    /**
     * 사용자 확인 문자열 검증.
     * "삭제" 입력 시에만 삭제 진행 허용.
     */
    public boolean verifyConfirmString(String inputString) {
        return "삭제".equals(inputString);
    }
}
