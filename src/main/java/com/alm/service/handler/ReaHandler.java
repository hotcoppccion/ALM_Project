package com.alm.service.handler;

import com.alm.dto.RealEstateDTO;
import com.alm.repository.AssetRepository;
import com.alm.util.ParseUtil;
import java.util.Map;

/**
 * [전략 패턴] 부동산(REA) 저장/수정 전략.
 */
public class ReaHandler implements AssetHandler {

    @Override
    public boolean save(long assetId, Map<String, Object> payload, AssetRepository repo) {
        RealEstateDTO dto = new RealEstateDTO();
        dto.setAsset_id(assetId);
        dto.setContract_type((String) payload.getOrDefault("contract_type", ""));
        dto.setAddress((String) payload.getOrDefault("address", ""));
        dto.setPrice(ParseUtil.parseLong(payload.get("price")));
        return repo.insertRealEstateDetails(dto);
    }

    @Override
    public boolean update(long assetId, Map<String, Object> payload, AssetRepository repo) {
        RealEstateDTO dto = new RealEstateDTO();
        dto.setAsset_id(assetId);
        dto.setContract_type((String) payload.getOrDefault("contract_type", ""));
        dto.setAddress((String) payload.getOrDefault("address", ""));
        dto.setPrice(ParseUtil.parseLong(payload.get("price")));
        return repo.updateRealEstateDetails(dto);
    }
}
