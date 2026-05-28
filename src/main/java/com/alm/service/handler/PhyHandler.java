package com.alm.service.handler;

import com.alm.dto.PhysicalAssetDTO;
import com.alm.repository.AssetRepository;
import com.alm.util.ParseUtil;
import java.util.Map;

/**
 * [전략 패턴] 실물자산(PHY) 저장/수정 전략.
 */
public class PhyHandler implements AssetHandler {

    @Override
    public boolean save(long assetId, Map<String, Object> payload, AssetRepository repo) {
        PhysicalAssetDTO dto = new PhysicalAssetDTO();
        dto.setAsset_id(assetId);
        dto.setItem_name((String) payload.getOrDefault("item_name", ""));
        dto.setPurchase_price(ParseUtil.parseLong(payload.get("purchase_price")));
        dto.setCurrent_value(ParseUtil.parseLong(payload.get("current_value")));
        return repo.insertPhysicalDetails(dto);
    }

    @Override
    public boolean update(long assetId, Map<String, Object> payload, AssetRepository repo) {
        PhysicalAssetDTO dto = new PhysicalAssetDTO();
        dto.setAsset_id(assetId);
        dto.setItem_name((String) payload.getOrDefault("item_name", ""));
        dto.setPurchase_price(ParseUtil.parseLong(payload.get("purchase_price")));
        dto.setCurrent_value(ParseUtil.parseLong(payload.get("current_value")));
        return repo.updatePhysicalDetails(dto);
    }
}
