package com.alm.service;

import com.alm.dto.AccountDTO;

/**
 * 금융계좌 특화 비즈니스 로직 (AssetService 보조).
 *
 * [설계 의도]
 *   AssetService 는 자산 전반(등록/수정/삭제/조회)을 담당.
 *   AccountService 는 계좌에 특화된 계산 로직만 분리 (SRP).
 */
public class AccountService {

    /**
     * 예상 연간 이자 수익금 계산.
     * 이자 수익금 = 잔액 × (이자율 / 100)
     *
     * @return 연간 이자 수익금 (소수점 포함). 이체 처리 시 long 으로 반올림 후 사용 권장.
     */
    public double calculateInterest(AccountDTO account) {
        if (account == null) return 0.0;
        return account.getBalance() * (account.getAccount_interest() / 100.0);
    }

    /**
     * 계좌 간 이체 (미구현 — 스텁).
     *
     * [구현 예정]
     *   두 UPDATE (차감 + 증가)를 하나의 트랜잭션으로 묶어야 한다.
     *   JDBC: conn.setAutoCommit(false) → 쿼리 2건 → commit / rollback.
     */
    public boolean transferBetweenAccounts(long fromAssetId, long toAssetId, long amount) {
        // TODO: 트랜잭션 기반 이체 구현
        return true;
    }
}
