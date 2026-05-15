package com.alm.dto;

public class AccountDTO extends AssetDTO {
    private String acc_number;
    private long balance;
    private double account_interest;

    // DB JOIN으로 가져올 문자열 필드
    private String bank_name;
    private String type_name;

    // 수정 모달 pre-fill용 ID 필드
    private int bank_id;
    private int type_id;

    public String getAcc_number() { return acc_number; }
    public void setAcc_number(String acc_number) { this.acc_number = acc_number; }

    public long getBalance() { return balance; }
    public void setBalance(long balance) { this.balance = balance; }

    public double getAccount_interest() { return account_interest; }
    public void setAccount_interest(double account_interest) { this.account_interest = account_interest; }

    public String getBank_name() { return bank_name; }
    public void setBank_name(String bank_name) { this.bank_name = bank_name; }

    public String getType_name() { return type_name; }
    public void setType_name(String type_name) { this.type_name = type_name; }

    public int getBank_id() { return bank_id; }
    public void setBank_id(int bank_id) { this.bank_id = bank_id; }

    public int getType_id() { return type_id; }
    public void setType_id(int type_id) { this.type_id = type_id; }
}