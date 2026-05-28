package com.alm.dto;

/**
 * 금융계좌(account_table) DTO.
 *
 * [설계 근거 — bank_id / type_id 분리 보유]
 *   수정 모달 pre-fill 시 드롭다운의 selected 값을 맞추려면
 *   bank_name 문자열이 아닌 bank_id(int) 가 필요하다.
 *   JOIN 결과인 bank_name / type_name 과 FK 값인 bank_id / type_id 를 함께 보유한다.
 */
public class AccountDTO extends AssetDTO {

    // account_table 컬럼
    private String acc_number;
    private long   balance;
    private double account_interest; // 연이자율 (%). 소수점 처리를 위해 double.

    // JOIN 결과 (LEFT JOIN — bank_id 가 NULL 인 계좌도 목록에 표시해야 하므로 LEFT)
    private String bank_name;
    private String type_name;

    // 수정 모달 드롭다운 selected 값 복원용
    private int bank_id;
    private int type_id;

    public String getAcc_number()                          { return acc_number; }
    public void   setAcc_number(String v)                  { this.acc_number = v; }
    public long   getBalance()                             { return balance; }
    public void   setBalance(long v)                       { this.balance = v; }
    public double getAccount_interest()                    { return account_interest; }
    public void   setAccount_interest(double v)            { this.account_interest = v; }
    public String getBank_name()                           { return bank_name; }
    public void   setBank_name(String v)                   { this.bank_name = v; }
    public String getType_name()                           { return type_name; }
    public void   setType_name(String v)                   { this.type_name = v; }
    public int    getBank_id()                             { return bank_id; }
    public void   setBank_id(int v)                        { this.bank_id = v; }
    public int    getType_id()                             { return type_id; }
    public void   setType_id(int v)                        { this.type_id = v; }

    @Override
    public long getAmount() { return balance; }
}
