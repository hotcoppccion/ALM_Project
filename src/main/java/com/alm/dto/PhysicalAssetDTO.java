package com.alm.dto;

public class PhysicalAssetDTO extends AssetDTO {
    private String item_name;
    private long purchase_price;
    private long current_value;
    private java.sql.Date last_updated;

    public String getItem_name() { return item_name; }
    public void setItem_name(String item_name) { this.item_name = item_name; }

    public long getPurchase_price() { return purchase_price; }
    public void setPurchase_price(long purchase_price) { this.purchase_price = purchase_price; }

    public long getCurrent_value() { return current_value; }
    public void setCurrent_value(long current_value) { this.current_value = current_value; }

    public java.sql.Date getLast_updated() { return last_updated; }
    public void setLast_updated(java.sql.Date last_updated) { this.last_updated = last_updated; }
}