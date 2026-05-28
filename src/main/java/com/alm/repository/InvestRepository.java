package com.alm.repository;

import com.alm.dto.InvestLogDTO;
import com.alm.dto.InvestPortfolioDTO;
import com.alm.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 투자 포트폴리오 + 매매 일지 DB 접근 계층.
 *
 * [stock_master 참조 없음]
 *   ticker_code / ticker_name 을 invest_portfolio / invest_log 에 직접 저장하므로
 *   stock_master JOIN 이 없다.
 *
 * [크로스 도메인 잔액 갱신]
 *   매수/매도 시 account_table.balance 를 이 Repository 에서 직접 UPDATE 한다.
 *   단순 프로젝트이므로 AssetRepository 를 거치지 않고 여기서 처리.
 */
public class InvestRepository {

    // ── 포트폴리오 조회 ───────────────────────────────────────────────

    public List<InvestPortfolioDTO> findAllPortfolio() {
        String sql = "SELECT invest_id, asset_id, ticker_code, ticker_name, "
                   + "       quantity, purchase_price "
                   + "FROM invest_portfolio "
                   + "ORDER BY ticker_code";

        List<InvestPortfolioDTO> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapPortfolio(rs));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /** (계좌 ID + 종목 코드) 복합키로 단건 조회. 같은 종목을 여러 계좌에서 보유할 수 있어 asset_id 필수. */
    public InvestPortfolioDTO findPortfolioByKey(int assetId, String tickerCode) {
        String sql = "SELECT invest_id, asset_id, ticker_code, ticker_name, "
                   + "       quantity, purchase_price "
                   + "FROM invest_portfolio "
                   + "WHERE asset_id = ? AND ticker_code = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, assetId);
            ps.setString(2, tickerCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapPortfolio(rs);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    /**
     * ticker_code 단독 조회 (매도 시 계좌 자동 특정용).
     * 같은 종목을 여러 계좌에 보유한 경우 가장 먼저 매수한 계좌 반환.
     */
    public InvestPortfolioDTO findPortfolioByTicker(String tickerCode) {
        String sql = "SELECT invest_id, asset_id, ticker_code, ticker_name, "
                   + "       quantity, purchase_price "
                   + "FROM invest_portfolio "
                   + "WHERE ticker_code = ? "
                   + "LIMIT 1";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, tickerCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapPortfolio(rs);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    // ── 매수 ─────────────────────────────────────────────────────────

    /**
     * 포트폴리오 Upsert (신규 INSERT or 기존 가중평균단가 재계산 후 UPDATE).
     *
     * [가중평균단가(WAC) 계산]
     *   (기존수량 × 기존단가 + 신규수량 × 신규단가) ÷ (기존수량 + 신규수량)
     *   원 단위 정수 나눗셈(소수점 버림) — 은행 관행과 동일.
     */
    public void buyStock(int assetId, String tickerCode, String tickerName,
                         int qty, long price) {

        InvestPortfolioDTO existing = findPortfolioByKey(assetId, tickerCode);

        if (existing == null) {
            String sql = "INSERT INTO invest_portfolio "
                       + "(asset_id, ticker_code, ticker_name, quantity, purchase_price) "
                       + "VALUES (?, ?, ?, ?, ?)";
            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1,    assetId);
                ps.setString(2, tickerCode);
                ps.setString(3, tickerName);
                ps.setInt(4,    qty);
                ps.setLong(5,   price);
                ps.executeUpdate();
            } catch (Exception e) { e.printStackTrace(); }

        } else {
            long oldQty   = existing.getQuantity();
            long oldPrice = existing.getPurchase_price();
            long newAvg   = (oldQty * oldPrice + (long) qty * price) / (oldQty + qty);

            String sql = "UPDATE invest_portfolio "
                       + "SET quantity = ?, purchase_price = ? "
                       + "WHERE asset_id = ? AND ticker_code = ?";
            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1,    (int)(oldQty + qty));
                ps.setLong(2,   newAvg);
                ps.setInt(3,    assetId);
                ps.setString(4, tickerCode);
                ps.executeUpdate();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    // ── 매도 ─────────────────────────────────────────────────────────

    /**
     * 수량 차감. 잔량 0 이하면 포트폴리오 행 삭제(청산).
     *
     * [실현손익]
     *   (매도단가 − 평균매입단가) × 매도수량. invest_log 에 기록.
     *
     * @return 실현손익 (양수 = 이익, 음수 = 손실). 보유 없으면 0L.
     */
    public long sellStock(int assetId, String tickerCode, int qty, long sellPrice) {
        InvestPortfolioDTO existing = findPortfolioByKey(assetId, tickerCode);
        if (existing == null) return 0L;

        long realizedProfit = (sellPrice - existing.getPurchase_price()) * qty;
        int  remainQty      = existing.getQuantity() - qty;

        if (remainQty <= 0) {
            String sql = "DELETE FROM invest_portfolio WHERE asset_id = ? AND ticker_code = ?";
            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, assetId);
                ps.setString(2, tickerCode);
                ps.executeUpdate();
            } catch (Exception e) { e.printStackTrace(); }
        } else {
            // 매도 후 평단가는 변하지 않으므로 수량만 업데이트
            String sql = "UPDATE invest_portfolio SET quantity = ? "
                       + "WHERE asset_id = ? AND ticker_code = ?";
            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, remainQty);
                ps.setInt(2, assetId);
                ps.setString(3, tickerCode);
                ps.executeUpdate();
            } catch (Exception e) { e.printStackTrace(); }
        }
        return realizedProfit;
    }

    // ── 계좌 잔액 갱신 ────────────────────────────────────────────────

    /**
     * 매수/매도 후 예수금 갱신.
     * SET balance = balance + delta : 원자적 연산으로 레이스 컨디션 방지.
     *
     * @param delta 매수 시 음수(차감), 매도 시 양수(증가)
     */
    public void updateAccountBalance(int assetId, long delta) {
        String sql = "UPDATE account_table SET balance = balance + ? WHERE asset_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, delta);
            ps.setInt(2,  assetId);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── 매매 일지 ─────────────────────────────────────────────────────

    public List<InvestLogDTO> findAllLogs() {
        String sql = "SELECT invest_log_id, asset_id, ticker_code, ticker_name, "
                   + "       transaction_type, quantity, price, realized_profit, "
                   + "       reason_basis, DATE_FORMAT(trade_date, '%Y-%m-%d') AS trade_date "
                   + "FROM invest_log "
                   + "ORDER BY trade_date DESC, invest_log_id DESC";

        List<InvestLogDTO> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                InvestLogDTO dto = new InvestLogDTO();
                dto.setInvest_log_id(rs.getInt("invest_log_id"));
                dto.setAsset_id(rs.getInt("asset_id"));
                dto.setTicker_code(rs.getString("ticker_code"));
                dto.setTicker_name(rs.getString("ticker_name"));
                dto.setTransaction_type(rs.getString("transaction_type"));
                dto.setQuantity(rs.getInt("quantity"));
                dto.setPrice(rs.getLong("price"));
                dto.setRealized_profit(rs.getLong("realized_profit"));
                dto.setReason_basis(rs.getString("reason_basis"));
                dto.setTrade_date(rs.getString("trade_date"));
                list.add(dto);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    /**
     * 매매 이력 등록. 매수(BUY) / 매도(SELL) 공용.
     * reason 이 빈 문자열이면 NULL 저장 (의미 없는 빈 TEXT 방지).
     */
    public void insertLog(int assetId, String tickerCode, String tickerName,
                          String type, int qty, long price,
                          long profit, String reason, String date) {
        String sql = "INSERT INTO invest_log "
                   + "(asset_id, ticker_code, ticker_name, transaction_type, "
                   + " quantity, price, realized_profit, reason_basis, trade_date) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1,    assetId);
            ps.setString(2, tickerCode);
            ps.setString(3, tickerName);
            ps.setString(4, type);
            ps.setInt(5,    qty);
            ps.setLong(6,   price);
            ps.setLong(7,   profit);
            if (reason == null || reason.isEmpty()) ps.setNull(8, Types.VARCHAR);
            else                                    ps.setString(8, reason);
            ps.setString(9, date);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void deleteLog(int logId) {
        String sql = "DELETE FROM invest_log WHERE invest_log_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, logId);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── 증권 계좌 목록 (드롭다운용) ──────────────────────────────────

    /**
     * 증권위탁계좌 + ISA 목록 반환 (매수 화면 계좌 선택용).
     *
     * [LEFT JOIN account_bank]
     *   bank_id 가 NULL 인 계좌도 목록에 포함되어야 하므로 LEFT JOIN.
     *   COALESCE(b.bank_name, '미지정') : bank_name 이 NULL 이면 '미지정' 으로 표시.
     *
     * [LinkedHashMap]
     *   JSON 응답의 키 순서를 삽입 순서대로 유지하기 위해 사용.
     */
    public List<Map<String, Object>> findBrokerageAccounts() {
        String sql = "SELECT ac.asset_id, "
                   + "       COALESCE(b.bank_name, '미지정') AS bank_name, "
                   + "       ac.acc_number, "
                   + "       ac.balance "
                   + "FROM account_table ac "
                   + "JOIN  asset_master  a ON ac.asset_id = a.asset_id "
                   + "JOIN  account_type  t ON ac.type_id  = t.type_id "
                   + "LEFT JOIN account_bank b ON ac.bank_id = b.bank_id "
                   + "WHERE t.type_name IN ('증권위탁계좌', 'ISA(개인종합자산관리)') "
                   + "ORDER BY ac.asset_id";

        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("asset_id",   rs.getInt("asset_id"));
                row.put("bank_name",  rs.getString("bank_name"));
                row.put("acc_number", rs.getString("acc_number"));
                row.put("balance",    rs.getLong("balance"));
                list.add(row);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    // ── 총 포트폴리오 평가액 ──────────────────────────────────────────

    /**
     * 보유 주식 전체의 매입 평가액(quantity × purchase_price) 합계.
     * 실시간 시세 아닌 평균매입단가 기준 → KIS API 호출 없음.
     * AssetService.calculateTotalAsset() 에서 예수금과 합산해 총 투자 자산을 구한다.
     */
    public long getTotalPortfolioValue() {
        String sql = "SELECT COALESCE(SUM(quantity * purchase_price), 0) FROM invest_portfolio";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getLong(1);
        } catch (Exception e) {
            System.err.println("[ERROR] 포트폴리오 평가액 조회 실패: " + e.getMessage());
        }
        return 0L;
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────

    private InvestPortfolioDTO mapPortfolio(ResultSet rs) throws SQLException {
        InvestPortfolioDTO dto = new InvestPortfolioDTO();
        dto.setInvest_id(rs.getInt("invest_id"));
        dto.setAsset_id(rs.getInt("asset_id"));
        dto.setTicker_code(rs.getString("ticker_code"));
        dto.setTicker_name(rs.getString("ticker_name"));
        dto.setQuantity(rs.getInt("quantity"));
        dto.setPurchase_price(rs.getLong("purchase_price"));
        return dto;
    }
}
