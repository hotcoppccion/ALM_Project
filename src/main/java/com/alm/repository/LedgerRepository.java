package com.alm.repository;

import com.alm.dto.GeneralLedgerDTO;
import com.alm.util.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 가계부 도메인 DB 접근 계층.
 *
 * [DB 구조 — Class Table Inheritance]
 *   ledger_master (공통: ledger_id AUTO_INCREMENT, type_code, created_at)
 *   general_ledger (ledger_id PK/FK, asset_id, category_id, amount, transaction_date)
 *
 *   저장: ledger_master INSERT → 발급된 ledger_id 로 general_ledger INSERT.
 *   삭제: ledger_master DELETE 만으로 충분 — general_ledger 에 ON DELETE CASCADE 적용.
 */
public class LedgerRepository {

    // ── INSERT ────────────────────────────────────────────────────────

    /**
     * ledger_master 에 부모 레코드를 삽입하고 AUTO_INCREMENT ledger_id 반환.
     * general_ledger INSERT 전에 반드시 먼저 호출해야 한다.
     *
     * @return 발급된 ledger_id, 실패 시 -1
     */
    public int insertLedgerMaster(String typeCode) {
        String sql = "INSERT INTO ledger_master (type_code, created_at) VALUES (?, NOW())";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, typeCode);
            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] ledger_master 생성 실패: " + e.getMessage());
        }
        return -1;
    }

    /**
     * general_ledger 에 거래 내역 삽입.
     * asset_id 가 0 이면 NULL 저장 (FK 제약 위반 방지, 자산 미연동 내역).
     */
    public boolean insertGeneralLedger(GeneralLedgerDTO dto) {
        String sql = "INSERT INTO general_ledger (ledger_id, asset_id, category_id, amount, transaction_date) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, dto.getLedger_id());
            if (dto.getAsset_id() > 0) pstmt.setLong(2, dto.getAsset_id());
            else                       pstmt.setNull(2, java.sql.Types.BIGINT);
            pstmt.setInt(3, dto.getCategory_id());
            pstmt.setLong(4, dto.getAmount());
            pstmt.setDate(5, java.sql.Date.valueOf(dto.getTransaction_date()));

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ERROR] general_ledger 저장 실패: " + e.getMessage());
            return false;
        }
    }

    // ── SELECT ────────────────────────────────────────────────────────

    /**
     * 전체 가계부 내역 조회 (최신순).
     *
     * [asset_name 구성]
     *   ACC: "은행명 (계좌번호 뒤 4자리)" — 보안상 전체 번호 미노출.
     *   CSH: cash_asset.name 그대로 사용.
     *   COALESCE 로 ACC/CSH 중 실제로 연결된 쪽을 선택.
     */
    public List<GeneralLedgerDTO> findAllGeneralLedger() {
        List<GeneralLedgerDTO> list = new ArrayList<>();

        String sql = "SELECT gl.ledger_id, lm.type_code, gl.asset_id, gl.category_id, " +
                     "gl.amount, DATE_FORMAT(gl.transaction_date, '%Y-%m-%d') AS transaction_date, " +
                     "lc.category_name, " +
                     "COALESCE(CONCAT(ab.bank_name, ' (', SUBSTR(at2.acc_number, -4), ')'), ca.name) AS asset_name " +
                     "FROM general_ledger gl " +
                     "JOIN ledger_master lm ON gl.ledger_id = lm.ledger_id " +
                     "LEFT JOIN ledger_category lc ON gl.category_id = lc.category_id " +
                     "LEFT JOIN account_table at2 ON gl.asset_id = at2.asset_id " +
                     "LEFT JOIN account_bank ab ON at2.bank_id = ab.bank_id " +
                     "LEFT JOIN cash_asset ca ON gl.asset_id = ca.asset_id " +
                     "ORDER BY gl.transaction_date DESC, gl.ledger_id DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                GeneralLedgerDTO dto = new GeneralLedgerDTO();
                dto.setLedger_id(rs.getInt("ledger_id"));
                dto.setType_code(rs.getString("type_code"));
                dto.setAsset_id(rs.getLong("asset_id"));
                dto.setCategory_id(rs.getInt("category_id"));
                dto.setAmount(rs.getLong("amount"));
                dto.setTransaction_date(rs.getString("transaction_date"));
                dto.setCategory_name(rs.getString("category_name"));
                dto.setAsset_name(rs.getString("asset_name"));
                list.add(dto);
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] general_ledger 목록 조회 실패: " + e.getMessage());
        }
        return list;
    }

    /**
     * 삭제 전 잔액 복원을 위해 단건 조회.
     * 삭제 후에는 데이터가 사라지므로 반드시 삭제 전에 호출해야 한다.
     *
     * @return asset_id, amount 가 채워진 DTO. 없으면 null.
     */
    public GeneralLedgerDTO findGeneralLedgerById(int ledgerId) {
        String sql = "SELECT ledger_id, asset_id, amount FROM general_ledger WHERE ledger_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, ledgerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    GeneralLedgerDTO dto = new GeneralLedgerDTO();
                    dto.setLedger_id(rs.getInt("ledger_id"));
                    dto.setAsset_id(rs.getLong("asset_id"));
                    dto.setAmount(rs.getLong("amount"));
                    return dto;
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] general_ledger 단건 조회 실패: " + e.getMessage());
        }
        return null;
    }

    /**
     * 이달(현재 연월) 수입 합계, 지출 합계, 순수지를 반환.
     *
     * @return {"totalIncome", "totalExpense", "net"}
     */
    public Map<String, Long> getSumByCategory() {
        Map<String, Long> result = new HashMap<>();

        String sql = "SELECT " +
                     "COALESCE(SUM(CASE WHEN amount > 0 THEN amount ELSE 0 END), 0) AS total_income, " +
                     "COALESCE(SUM(CASE WHEN amount < 0 THEN ABS(amount) ELSE 0 END), 0) AS total_expense " +
                     "FROM general_ledger " +
                     "WHERE YEAR(transaction_date) = YEAR(CURDATE()) " +
                     "AND MONTH(transaction_date) = MONTH(CURDATE())";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                long income  = rs.getLong("total_income");
                long expense = rs.getLong("total_expense");
                result.put("totalIncome",  income);
                result.put("totalExpense", expense);
                result.put("net",          income - expense);
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] 월별 요약 조회 실패: " + e.getMessage());
        }
        return result;
    }

    public List<Map<String, Object>> findAllCategories() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT category_id, category_name FROM ledger_category ORDER BY category_id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("category_id",   rs.getInt("category_id"));
                row.put("category_name", rs.getString("category_name"));
                list.add(row);
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] 카테고리 목록 조회 실패: " + e.getMessage());
        }
        return list;
    }

    /** @return 발급된 category_id, 실패 시 -1 */
    public int insertCategory(String categoryName) {
        String sql = "INSERT INTO ledger_category (category_name) VALUES (?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, categoryName);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] 카테고리 추가 실패: " + e.getMessage());
        }
        return -1;
    }

    // ── DELETE ────────────────────────────────────────────────────────

    /** ledger_master 삭제. general_ledger 는 ON DELETE CASCADE 로 자동 삭제. */
    public boolean deleteLedgerMaster(int ledgerId) {
        String sql = "DELETE FROM ledger_master WHERE ledger_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, ledgerId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ERROR] ledger_master 삭제 실패: " + e.getMessage());
            return false;
        }
    }

    // ── 잔액 조정 ─────────────────────────────────────────────────────

    /**
     * 연동 자산(ACC/CSH)의 잔액을 delta 만큼 증감.
     *
     * [ACC/CSH 구분 없이 두 테이블 모두 시도하는 이유]
     *   asset_id 만으로 자산 타입을 별도 조회하지 않아도 된다.
     *   해당 테이블에 asset_id 가 없으면 WHERE 불일치로 0행 업데이트 → 무해함.
     *
     * [SET balance = balance + delta 패턴]
     *   읽기→계산→쓰기 3단계 대신 DB 내부 원자적 연산으로 처리.
     *
     * @param delta 양수 = 잔액 증가(수입), 음수 = 잔액 감소(지출)
     */
    public boolean adjustAssetBalance(long assetId, long delta) {
        boolean updated = false;

        String accSql = "UPDATE account_table SET balance = balance + ? WHERE asset_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(accSql)) {
            pstmt.setLong(1, delta);
            pstmt.setLong(2, assetId);
            if (pstmt.executeUpdate() > 0) updated = true;
        } catch (SQLException e) {
            System.err.println("[ERROR] account_table 잔액 조정 실패: " + e.getMessage());
        }

        String cshSql = "UPDATE cash_asset SET balance = balance + ? WHERE asset_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(cshSql)) {
            pstmt.setLong(1, delta);
            pstmt.setLong(2, assetId);
            if (pstmt.executeUpdate() > 0) updated = true;
        } catch (SQLException e) {
            System.err.println("[ERROR] cash_asset 잔액 조정 실패: " + e.getMessage());
        }

        return updated;
    }

    /**
     * 가계부 내역 등록 폼의 자산 드롭다운용 — ACC + CSH 타입만 반환.
     * REA(부동산), PHY(실물)는 유동 잔액이 없으므로 제외.
     */
    public List<Map<String, Object>> findLiquidAssets() {
        List<Map<String, Object>> list = new ArrayList<>();

        String accSql = "SELECT m.asset_id, m.type_code, " +
                        "CONCAT(b.bank_name, ' (', SUBSTR(a.acc_number, -4), ')') AS display_name " +
                        "FROM asset_master m " +
                        "JOIN account_table a ON m.asset_id = a.asset_id " +
                        "JOIN account_bank b ON a.bank_id = b.bank_id " +
                        "WHERE m.type_code = 'ACC' ORDER BY m.asset_id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(accSql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("asset_id",     rs.getLong("asset_id"));
                row.put("type_code",    rs.getString("type_code"));
                row.put("display_name", rs.getString("display_name"));
                list.add(row);
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] ACC 자산 목록 조회 실패: " + e.getMessage());
        }

        String cshSql = "SELECT m.asset_id, m.type_code, c.name AS display_name " +
                        "FROM asset_master m JOIN cash_asset c ON m.asset_id = c.asset_id " +
                        "WHERE m.type_code = 'CSH' ORDER BY m.asset_id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(cshSql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("asset_id",     rs.getLong("asset_id"));
                row.put("type_code",    rs.getString("type_code"));
                row.put("display_name", rs.getString("display_name"));
                list.add(row);
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] CSH 자산 목록 조회 실패: " + e.getMessage());
        }

        return list;
    }

    /** 자산 삭제 가능 여부 판단용 — 참조 내역 존재 여부 확인. */
    public boolean existsByAssetId(long assetId) {
        String sql = "SELECT COUNT(*) FROM general_ledger WHERE asset_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, assetId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] 가계부 참조 여부 확인 실패: " + e.getMessage());
        }
        return false;
    }
}
