package com.alm.dto;

/**
 * 투자 포트폴리오 DTO (보유 종목 한 행).
 *
 * [DB 저장 필드 vs 계산 필드]
 *   DB 저장 필드 : invest_portfolio 테이블 컬럼 직접 매핑.
 *   계산 필드    : InvestService 가 KIS OpenAPI 호출 후 set. Repository 조회 시점엔 0/false 기본값.
 *
 * [stock_master 미사용]
 *   ticker_code / ticker_name 을 invest_portfolio 에 직접 저장 → JOIN 없이 단일 테이블로 완결.
 *
 * [asset_id 포함 이유]
 *   동일 종목을 여러 계좌에 분산 보유할 수 있으므로,
 *   매수/매도 시 어느 계좌(asset_id)의 balance 를 차감/증가할지 특정하기 위해 포함.
 */
public class InvestPortfolioDTO {

    // ── DB 저장 필드 (invest_portfolio 컬럼) ──────────────────────────
    private int    invest_id;      // 포트폴리오 항목 고유 ID (AUTO_INCREMENT PK)
    private int    asset_id;       // 연동 증권위탁계좌 ID (account_table.asset_id)
    private String ticker_code;    // 종목 코드 (예: "005930", "AAPL")
    private String ticker_name;    // 종목명 (예: "삼성전자", "Apple Inc.")
    private int    quantity;       // 보유 수량 (주)
    private long   purchase_price; // 가중 평균 매입 단가 (원)

    // ── 계산 필드 (InvestService 가 KIS API 호출 후 set) ─────────────
    private long    current_price;  // KIS API 현재가 (원). API 실패 시 -1
    private long    current_value;  // 현재 평가액 = current_price × quantity
    private long    book_value;     // 장부가 = purchase_price × quantity
    private long    profit_loss;    // 평가 손익 = current_value - book_value (음수 = 손실)
    private double  profit_rate;    // 수익률 (%) = profit_loss / book_value × 100
    private String  price_change;   // 전일 대비 등락률 (%, KIS API 응답 문자열 그대로)
    private boolean api_error;      // KIS API 조회 실패 여부 (실패 시 true)

    // ── Getters ──────────────────────────────────────────────────────
    public int     getInvest_id()      { return invest_id; }
    public int     getAsset_id()       { return asset_id; }
    public String  getTicker_code()    { return ticker_code; }
    public String  getTicker_name()    { return ticker_name; }
    public int     getQuantity()       { return quantity; }
    public long    getPurchase_price() { return purchase_price; }
    public long    getCurrent_price()  { return current_price; }
    public long    getCurrent_value()  { return current_value; }
    public long    getBook_value()     { return book_value; }
    public long    getProfit_loss()    { return profit_loss; }
    public double  getProfit_rate()    { return profit_rate; }
    public String  getPrice_change()   { return price_change; }
    public boolean isApi_error()       { return api_error; }

    // ── Setters ──────────────────────────────────────────────────────
    public void setInvest_id(int v)       { invest_id      = v; }
    public void setAsset_id(int v)        { asset_id       = v; }
    public void setTicker_code(String v)  { ticker_code    = v; }
    public void setTicker_name(String v)  { ticker_name    = v; }
    public void setQuantity(int v)        { quantity       = v; }
    public void setPurchase_price(long v) { purchase_price = v; }
    public void setCurrent_price(long v)  { current_price  = v; }
    public void setCurrent_value(long v)  { current_value  = v; }
    public void setBook_value(long v)     { book_value     = v; }
    public void setProfit_loss(long v)    { profit_loss    = v; }
    public void setProfit_rate(double v)  { profit_rate    = v; }
    public void setPrice_change(String v) { price_change   = v; }
    public void setApi_error(boolean v)   { api_error      = v; }
}
