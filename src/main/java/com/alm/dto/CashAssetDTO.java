package com.alm.dto;

public class CashAssetDTO extends AssetDTO {
    private String name;
    private long balance;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public long getBalance() { return balance; }
    public void setBalance(long balance) { this.balance = balance; }
}