package com.alm.util;

/** 주식 정보 조회 인터페이스. API 교체 시 새 구현체만 추가하면 된다. */
public interface StockInfoProvider {

    /** @return { 현재가, 전일대비등락률(%), 종목명, 시장구분 } */
    String[] getStockInfo(String tickerCode) throws Exception;

    /** @return { 현재값, 등락률(%) } */
    String[] getKospiIndex() throws Exception;
}
