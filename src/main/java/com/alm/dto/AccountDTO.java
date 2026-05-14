package com.alm.dto;

public class AccountDTO extends AssetDTO {
    private String acc_number;
    private long balance;
    private double account_interest;

    // DB JOIN으로 가져올 문자열 필드
    private String bank_name;
    private String type_name;

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
}