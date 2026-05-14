package com.alm.dto;

public abstract class AssetDTO {
    private long asset_id;
    private String type_code;

    public long getAsset_id() { return asset_id; }
    public void setAsset_id(long asset_id) { this.asset_id = asset_id; }

    public String getType_code() { return type_code; }
    public void setType_code(String type_code) { this.type_code = type_code; }
}