package com.alm.service;

import com.alm.dto.AccountDTO;

/** 금융계좌 이자 계산 서비스. */
public class AccountService {

    /**
     * 예상 연간 이자 수익금 계산.
     * 이자 수익금 = 잔액 × (이자율 / 100)
     *
     * @return 연간 이자 수익금 (원, 소수점 포함)
     */
    public double calculateInterest(AccountDTO account) {
        if (account == null) return 0.0;
        return account.getBalance() * (account.getAccount_interest() / 100.0);
    }

}
