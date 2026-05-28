package com.alm.util;

/**
 * KIS OpenAPI → StockInfoProvider 어댑터.
 *
 * [설계 근거]
 *   APIClient 는 static 메서드로 설계되어 있어 인터페이스를 직접 implements 할 수 없다.
 *   이 클래스가 인스턴스 메서드로 감싸 StockInfoProvider 규격을 맞춰준다.
 *   향후 다른 API(야후 파이낸스 등)로 교체하려면 새 Adapter 클래스를 만들어
 *   InvestService 생성자에 주입하면 된다.
 */
public class KisApiAdapter implements StockInfoProvider {

    @Override
    public String[] getStockInfo(String tickerCode) throws Exception {
        return APIClient.getStockInfo(tickerCode);
    }

    @Override
    public String[] getKospiIndex() throws Exception {
        return APIClient.getKospiIndex();
    }
}
