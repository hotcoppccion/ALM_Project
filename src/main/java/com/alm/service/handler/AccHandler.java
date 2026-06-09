package com.alm.service.handler;

import com.alm.dto.AccountDTO;
import com.alm.repository.AssetRepository;
import com.alm.util.ParseUtil;
import java.util.Map;

/** 금융계좌(ACC) 저장/수정 핸들러. */
public class AccHandler implements AssetHandler {

    @Override
    public boolean save(long assetId, Map<String, Object> payload, AssetRepository repo) {
        AccountDTO dto = new AccountDTO();
        dto.setAsset_id(assetId);
        dto.setAcc_number((String) payload.getOrDefault("acc_number", ""));
        dto.setBalance(ParseUtil.parseLong(payload.get("balance")));
        dto.setAccount_interest(ParseUtil.parseDouble(payload.get("account_interest")));
        int bankId = ParseUtil.parseInt(payload.get("bank_id"), 1);
        int typeId = ParseUtil.parseInt(payload.get("type_id"), 1);
        return repo.insertAccountDetails(dto, bankId, typeId);
    }

    @Override
    public boolean update(long assetId, Map<String, Object> payload, AssetRepository repo) {
        AccountDTO dto = new AccountDTO();
        dto.setAsset_id(assetId);
        dto.setAcc_number((String) payload.getOrDefault("acc_number", ""));
        dto.setBalance(ParseUtil.parseLong(payload.get("balance")));
        dto.setAccount_interest(ParseUtil.parseDouble(payload.get("account_interest")));
        int bankId = ParseUtil.parseInt(payload.get("bank_id"), 1);
        int typeId = ParseUtil.parseInt(payload.get("type_id"), 1);
        return repo.updateAccountDetails(dto, bankId, typeId);
    }
}
