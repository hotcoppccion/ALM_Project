package com.alm.dto;

/**
 * 투자 매매 이력 DTO (invest_log 테이블 매핑).
 *
 * [ticker_code / ticker_name 직접 저장]
 *   stock_master 테이블 미사용 → ticker_code + ticker_name 을 거래 시점 기준으로 직접 저장.
 *   상장폐지·종목명 변경 이후에도 거래 당시 이름이 그대로 보존된다.
 *
 * [asset_id 포함 이유]
 *   어느 증권 계좌에서 발생한 거래인지 식별하기 위해 필요.
 *   invest_portfolio.asset_id 와 동일 계좌를 가리킨다.
 */
public class InvestLogDTO {

    private int    invest_log_id;     // 매매 이력 고유 ID (AUTO_INCREMENT PK)
    private int    asset_id;          // 거래 증권계좌 ID (account_table.asset_id)
    private String ticker_code;       // 종목 코드 (예: "005930", "AAPL")
    private String ticker_name;       // 종목명 (거래 당시 이름 그대로 저장)
    private String transaction_type;  // 거래 유형: "BUY" 또는 "SELL"
    private int    quantity;          // 거래 수량 (주)
    private long   price;             // 거래 단가 (원)
    private long   realized_profit;   // 실현 손익 (매도 시 계산, 매수 시 0)
    private String reason_basis;      // 매매 근거 메모 (null 허용)
    private String trade_date;        // 거래 날짜 (YYYY-MM-DD 문자열)

    // ── Getters ──────────────────────────────────────────────────────
    public int    getInvest_log_id()     { return invest_log_id; }
    public int    getAsset_id()          { return asset_id; }
    public String getTicker_code()       { return ticker_code; }
    public String getTicker_name()       { return ticker_name; }
    public String getTransaction_type()  { return transaction_type; }
    public int    getQuantity()          { return quantity; }
    public long   getPrice()             { return price; }
    public long   getRealized_profit()   { return realized_profit; }
    public String getReason_basis()      { return reason_basis; }
    public String getTrade_date()        { return trade_date; }

    // ── Setters ──────────────────────────────────────────────────────
    public void setInvest_log_id(int v)       { invest_log_id    = v; }
    public void setAsset_id(int v)            { asset_id         = v; }
    public void setTicker_code(String v)      { ticker_code      = v; }
    public void setTicker_name(String v)      { ticker_name      = v; }
    public void setTransaction_type(String v) { transaction_type = v; }
    public void setQuantity(int v)            { quantity         = v; }
    public void setPrice(long v)              { price            = v; }
    public void setRealized_profit(long v)    { realized_profit  = v; }
    public void setReason_basis(String v)     { reason_basis     = v; }
    public void setTrade_date(String v)       { trade_date       = v; }
}
