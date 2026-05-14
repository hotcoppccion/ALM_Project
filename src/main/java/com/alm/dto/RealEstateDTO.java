package com.alm.dto;

public class RealEstateDTO extends AssetDTO {
    private String contract_type;
    private String address;
    private long price;

    public String getContract_type() { return contract_type; }
    public void setContract_type(String contract_type) { this.contract_type = contract_type; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public long getPrice() { return price; }
    public void setPrice(long price) { this.price = price; }
}