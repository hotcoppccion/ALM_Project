package com.alm.service.handler;

import com.alm.dto.CashAssetDTO;
import com.alm.repository.AssetRepository;
import com.alm.util.ParseUtil;
import java.util.Map;

/**
 * [전략 패턴] 현금(CSH) 저장/수정 전략.
 */
public class CshHandler implements AssetHandler {

    @Override
    public boolean save(long assetId, Map<String, Object> payload, AssetRepository repo) {
        CashAssetDTO dto = new CashAssetDTO();
        dto.setAsset_id(assetId);
        dto.setName((String) payload.getOrDefault("name", ""));
        dto.setBalance(ParseUtil.parseLong(payload.get("balance")));
        return repo.insertCashDetails(dto);
    }

    @Override
    public boolean update(long assetId, Map<String, Object> payload, AssetRepository repo) {
        CashAssetDTO dto = new CashAssetDTO();
        dto.setAsset_id(assetId);
        dto.setName((String) payload.getOrDefault("name", ""));
        dto.setBalance(ParseUtil.parseLong(payload.get("balance")));
        return repo.updateCashDetails(dto);
    }
}
