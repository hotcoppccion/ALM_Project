package com.alm.service.handler;

import com.alm.repository.AssetRepository;
import java.util.Map;

/** 자산 타입별 저장·수정 전략 인터페이스. 새 자산 타입은 구현체 추가로 확장한다. */
public interface AssetHandler {
    boolean save(long assetId, Map<String, Object> payload, AssetRepository repo);
    boolean update(long assetId, Map<String, Object> payload, AssetRepository repo);
}
