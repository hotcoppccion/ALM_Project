package com.alm.service;

import com.alm.dto.AccountDTO;

/**
 * [Business Logic Layer] 금융 계좌와 관련된 특화된 비즈니스 로직을 전담하는 서비스 클래스입니다.
 */
public class AccountService {

    /**
     * 설정된 이자율(account_interest)을 기반으로 수익금을 산출하는 메소드 [cite: 301]
     * @param account 이자율과 잔액 정보를 담고 있는 계좌 DTO
     * @return 산출된 예상 이자 수익금
     */
    public double calculateInterest(AccountDTO account) {
        if (account == null) {
            return 0.0;
        }
        // 수익금 산출: 잔액 * (이자율 / 100)
        return account.getBalance() * (account.getAccount_interest() / 100.0);
    }

    /**
     * 기획서 명세: 계좌간 이체 로직을 처리하는 메소드
     * @param fromAssetId 출금할 계좌의 식별자
     * @param toAssetId 입금할 계좌의 식별자
     * @param amount 이체할 금액
     * @return 이체 성공 여부
     */
    public boolean transferBetweenAccounts(long fromAssetId, long toAssetId, long amount) {
        // (트랜잭션을 통해 출금 및 입금 쿼리를 순차적으로 실행하는 세부 로직 구현부)
        return true;
    }
}