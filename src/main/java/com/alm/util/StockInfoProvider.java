package com.alm.util;

/**
 * 주식 정보 제공자 인터페이스 (Adapter Pattern).
 *
 * [설계 근거]
 *   InvestService 가 APIClient static 메서드를 직접 호출하면
 *   KIS API 에 강결합되어 API 교체 시 Service 수정이 필요하고 단위 테스트가 불가능했다.
 *
 *   이 인터페이스를 중간에 두면:
 *   - KIS → 다른 API 교체 시 새 Adapter 클래스만 구현하면 되고, Service 는 수정 없다.
 *   - 테스트 시 Mock 구현체를 주입해 실제 API 호출 없이 검증 가능하다.
 */
public interface StockInfoProvider {

    /** @return { 현재가, 전일대비등락률(%), 종목명, 시장구분 } */
    String[] getStockInfo(String tickerCode) throws Exception;

    /** @return { 현재값, 등락률(%) } */
    String[] getKospiIndex() throws Exception;
}
