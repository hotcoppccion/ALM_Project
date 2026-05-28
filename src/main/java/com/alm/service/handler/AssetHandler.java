package com.alm.service.handler;

import com.alm.repository.AssetRepository;
import java.util.Map;

/**
 * 자산 저장/수정 전략 인터페이스 (Strategy Pattern).
 *
 * [설계 근거]
 *   기존 AssetService 의 saveAssetDetails() / updateAssetDetails() 는
 *   ACC / REA / PHY / CSH 타입마다 if-else 체인을 가지고 있었다.
 *   새 자산 타입 추가 시 Service 를 직접 수정해야 해서 OCP 위반이었다.
 *
 *   각 타입의 저장·수정 로직을 이 인터페이스를 구현하는 별도 클래스로 분리하고,
 *   AssetService 는 Map<String, AssetHandler> 로 타입 코드에 맞는 전략을 선택해 실행한다.
 *   새 타입은 핸들러 클래스 추가 + Map 등록만으로 확장 가능하다.
 */
public interface AssetHandler {
    boolean save(long assetId, Map<String, Object> payload, AssetRepository repo);
    boolean update(long assetId, Map<String, Object> payload, AssetRepository repo);
}
