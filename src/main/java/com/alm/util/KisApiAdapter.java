package com.alm.util;

/** APIClient(static) 를 StockInfoProvider 인터페이스 규격에 맞게 감싸는 어댑터. */
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
