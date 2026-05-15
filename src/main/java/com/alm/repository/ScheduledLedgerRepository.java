package com.alm.repository;

import com.alm.dto.FixedExpenseReceiptDTO;
import com.alm.dto.FixedExpenseRuleDTO;
import com.alm.dto.RegularIncomeReceiptDTO;
import com.alm.dto.RegularIncomeRuleDTO;
import com.alm.dto.VariableExpenseReceiptDTO;
import com.alm.dto.VariableExpenseRuleDTO;
import com.alm.util.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * [Repository 계층 - DAO]
 * 고정지출 / 정기수입 / 변동지출 3개 도메인의 규칙(Rule) + 영수증(Receipt) 테이블에
 * 직접 접근하여 SQL을 실행하는 클래스.
 *
 * [담당 테이블]
 *   - fixed_expense_rule / fixed_expense_receipt
 *   - regular_income_rule / regular_income_receipt
 *   - variable_expense_rule / variable_expense_receipt
 *
 * [설계 원칙]
 *   - try-with-resources: Connection/PreparedStatement/ResultSet 자동 close.
 *   - PreparedStatement: ? 플레이스홀더로 SQL Injection 방지.
 *   - ledger_master 조작(insertLedgerMaster, deleteLedgerMaster, adjustAssetBalance)은
 *     기존 LedgerRepository를 재사용 → 이 클래스에서는 각 전용 테이블 CRUD만 담당.
 */
public class ScheduledLedgerRepository {

    // ═══════════════════════════════════════════════════════════
    //  고정지출 규칙 (fixed_expense_rule)
    // ═══════════════════════════════════════════════════════════

    /**
     * 고정지출 규칙 전체 목록 조회.
     * LEFT JOIN ledger_category로 카테고리 이름을 함께 반환.
     *
     * @return FixedExpenseRuleDTO 리스트
     */
    public List<FixedExpenseRuleDTO> findAllFixedRules() {
        List<FixedExpenseRuleDTO> list = new ArrayList<>();
        String sql = "SELECT r.rule_id, r.name, r.category_id, r.amount, " +
                     "DATE_FORMAT(r.base_date, '%Y-%m-%d') AS base_date, " +
                     "r.p_value, r.p_unit, lc.category_name " +
                     "FROM fixed_expense_rule r " +
                     "LEFT JOIN ledger_category lc ON r.category_id = lc.category_id " +
                     "ORDER BY r.rule_id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                FixedExpenseRuleDTO dto = new FixedExpenseRuleDTO();
                dto.setRule_id(rs.getInt("rule_id"));
                dto.setName(rs.getString("name"));
                dto.setCategory_id(rs.getInt("category_id"));
                dto.setAmount(rs.getLong("amount"));
                dto.setBase_date(rs.getString("base_date"));
                dto.setP_value(rs.getInt("p_value"));
                dto.setP_unit(rs.getString("p_unit"));
                dto.setCategory_name(rs.getString("category_name"));
                list.add(dto);
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] 고정지출 규칙 목록 조회 실패: " + e.getMessage());
        }
        return list;
    }

    /**
     * 고정지출 규칙 단건 조회 (규칙 실행 시 amount 확인 용도).
     *
     * @param ruleId 조회할 규칙 ID
     * @return FixedExpenseRuleDTO, 없으면 null
     */
    public FixedExpenseRuleDTO findFixedRuleById(int ruleId) {
        String sql = "SELECT r.rule_id, r.name, r.category_id, r.amount, " +
                     "DATE_FORMAT(r.base_date, '%Y-%m-%d') AS base_date, " +
                     "r.p_value, r.p_unit, lc.category_name " +
                     "FROM fixed_expense_rule r " +
                     "LEFT JOIN ledger_category lc ON r.category_id = lc.category_id " +
                     "WHERE r.rule_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, ruleId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    FixedExpenseRuleDTO dto = new FixedExpenseRuleDTO();
                    dto.setRule_id(rs.getInt("rule_id"));
                    dto.setName(rs.getString("name"));
                    dto.setCategory_id(rs.getInt("category_id"));
                    dto.setAmount(rs.getLong("amount"));
                    dto.setBase_date(rs.getString("base_date"));
                    dto.setP_value(rs.getInt("p_value"));
                    dto.setP_unit(rs.getString("p_unit"));
                    dto.setCategory_name(rs.getString("category_name"));
                    return dto;
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] 고정지출 규칙 단건 조회 실패: " + e.getMessage());
        }
        return null;
    }

    /**
     * 고정지출 규칙 등록.
     *
     * @param dto name, category_id, amount, base_date, p_value, p_unit이 채워진 DTO
     * @return DB가 자동 발급한 rule_id, 실패 시 -1
     */
    public int insertFixedRule(FixedExpenseRuleDTO dto) {
        String sql = "INSERT INTO fixed_expense_rule (name, category_id, amount, base_date, p_value, p_unit) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, dto.getName());
            // category_id가 0이면 NULL 저장 (NULL 허용 컬럼)
            if (dto.getCategory_id() > 0) pstmt.setInt(2, dto.getCategory_id());
            else                          pstmt.setNull(2, Types.INTEGER);
            pstmt.setLong(3, dto.getAmount());
            pstmt.setDate(4, java.sql.Date.valueOf(dto.getBase_date()));
            pstmt.setInt(5, dto.getP_value());
            pstmt.setString(6, dto.getP_unit());
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] 고정지출 규칙 등록 실패: " + e.getMessage());
        }
        return -1;
    }

    /**
     * 고정지출 규칙 삭제.
     *
     * @param ruleId 삭제할 규칙 ID
     * @return 삭제 성공 시 true
     */
    public boolean deleteFixedRule(int ruleId) {
        String sql = "DELETE FROM fixed_expense_rule WHERE rule_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, ruleId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ERROR] 고정지출 규칙 삭제 실패: " + e.getMessage());
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────
    //  고정지출 영수증 (fixed_expense_receipt)
    // ─────────────────────────────────────────────────────────

    /**
     * 고정지출 영수증 전체 목록 조회.
     * JOIN: ledger_master, ledger_category, account_table+account_bank, cash_asset, fixed_expense_rule
     *
     * @return FixedExpenseReceiptDTO 리스트 (최신 날짜순)
     */
    public List<FixedExpenseReceiptDTO> findAllFixedReceipts() {
        List<FixedExpenseReceiptDTO> list = new ArrayList<>();
        String sql = "SELECT fr.ledger_id, lm.type_code, fr.rule_id, fr.asset_id, fr.category_id, " +
                     "fr.amount, DATE_FORMAT(fr.transaction_date, '%Y-%m-%d') AS transaction_date, " +
                     "lc.category_name, " +
                     "COALESCE(CONCAT(ab.bank_name, ' (', SUBSTR(at2.acc_number, -4), ')'), ca.name) AS asset_name, " +
                     "r.name AS rule_name " +
                     "FROM fixed_expense_receipt fr " +
                     "JOIN ledger_master lm ON fr.ledger_id = lm.ledger_id " +
                     "LEFT JOIN ledger_category lc ON fr.category_id = lc.category_id " +
                     "LEFT JOIN account_table at2 ON fr.asset_id = at2.asset_id " +
                     "LEFT JOIN account_bank ab ON at2.bank_id = ab.bank_id " +
                     "LEFT JOIN cash_asset ca ON fr.asset_id = ca.asset_id " +
                     "LEFT JOIN fixed_expense_rule r ON fr.rule_id = r.rule_id " +
                     "ORDER BY fr.transaction_date DESC, fr.ledger_id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                FixedExpenseReceiptDTO dto = new FixedExpenseReceiptDTO();
                dto.setLedger_id(rs.getInt("ledger_id"));
                dto.setType_code(rs.getString("type_code"));
                dto.setRule_id(rs.getInt("rule_id"));
                dto.setAsset_id(rs.getLong("asset_id"));
                dto.setCategory_id(rs.getInt("category_id"));
                dto.setAmount(rs.getLong("amount"));
                dto.setTransaction_date(rs.getString("transaction_date"));
                dto.setCategory_name(rs.getString("category_name"));
                dto.setAsset_name(rs.getString("asset_name"));
                dto.setRule_name(rs.getString("rule_name"));
                list.add(dto);
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] 고정지출 영수증 목록 조회 실패: " + e.getMessage());
        }
        return list;
    }

    /**
     * 고정지출 영수증 단건 조회 (삭제 전 자산 ID + 금액 복원 용도).
     *
     * @param ledgerId 조회할 내역 ID
     * @return FixedExpenseReceiptDTO (asset_id, amount만 채워짐), 없으면 null
     */
    public FixedExpenseReceiptDTO findFixedReceiptById(int ledgerId) {
        String sql = "SELECT ledger_id, asset_id, amount FROM fixed_expense_receipt WHERE ledger_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, ledgerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    FixedExpenseReceiptDTO dto = new FixedExpenseReceiptDTO();
                    dto.setLedger_id(rs.getInt("ledger_id"));
                    dto.setAsset_id(rs.getLong("asset_id"));
                    dto.setAmount(rs.getLong("amount"));
                    return dto;
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] 고정지출 영수증 단건 조회 실패: " + e.getMessage());
        }
        return null;
    }

    /**
     * 고정지출 영수증 삽입.
     * insertLedgerMaster()로 받은 ledger_id가 dto에 세팅되어야 함.
     *
     * @param dto ledger_id, rule_id, asset_id, category_id, amount, transaction_date 채워진 DTO
     * @return 삽입 성공 시 true
     */
    public boolean insertFixedReceipt(FixedExpenseReceiptDTO dto) {
        String sql = "INSERT INTO fixed_expense_receipt " +
                     "(ledger_id, asset_id, category_id, rule_id, amount, transaction_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, dto.getLedger_id());
            pstmt.setLong(2, dto.getAsset_id());
            if (dto.getCategory_id() > 0) pstmt.setInt(3, dto.getCategory_id());
            else                          pstmt.setNull(3, Types.INTEGER);
            pstmt.setInt(4, dto.getRule_id());
            pstmt.setLong(5, dto.getAmount());   // 지출이므로 음수
            pstmt.setDate(6, java.sql.Date.valueOf(dto.getTransaction_date()));
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ERROR] 고정지출 영수증 저장 실패: " + e.getMessage());
            return false;
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  정기수입 규칙 (regular_income_rule)
    // ═══════════════════════════════════════════════════════════

    /**
     * 정기수입 규칙 전체 목록 조회.
     *
     * @return RegularIncomeRuleDTO 리스트
     */
    public List<RegularIncomeRuleDTO> findAllIncomeRules() {
        List<RegularIncomeRuleDTO> list = new ArrayList<>();
        String sql = "SELECT r.rule_id, r.name, r.category_id, r.amount, " +
                     "DATE_FORMAT(r.base_date, '%Y-%m-%d') AS base_date, " +
                     "r.p_value, r.p_unit, lc.category_name " +
                     "FROM regular_income_rule r " +
                     "LEFT JOIN ledger_category lc ON r.category_id = lc.category_id " +
                     "ORDER BY r.rule_id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                RegularIncomeRuleDTO dto = new RegularIncomeRuleDTO();
                dto.setRule_id(rs.getInt("rule_id"));
                dto.setName(rs.getString("name"));
                dto.setCategory_id(rs.getInt("category_id"));
                dto.setAmount(rs.getLong("amount"));
                dto.setBase_date(rs.getString("base_date"));
                dto.setP_value(rs.getInt("p_value"));
                dto.setP_unit(rs.getString("p_unit"));
                dto.setCategory_name(rs.getString("category_name"));
                list.add(dto);
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] 정기수입 규칙 목록 조회 실패: " + e.getMessage());
        }
        return list;
    }

    /**
     * 정기수입 규칙 단건 조회.
     *
     * @param ruleId 조회할 규칙 ID
     * @return RegularIncomeRuleDTO, 없으면 null
     */
    public RegularIncomeRuleDTO findIncomeRuleById(int ruleId) {
        String sql = "SELECT r.rule_id, r.name, r.category_id, r.amount, " +
                     "DATE_FORMAT(r.base_date, '%Y-%m-%d') AS base_date, " +
                     "r.p_value, r.p_unit, lc.category_name " +
                     "FROM regular_income_rule r " +
                     "LEFT JOIN ledger_category lc ON r.category_id = lc.category_id " +
                     "WHERE r.rule_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, ruleId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    RegularIncomeRuleDTO dto = new RegularIncomeRuleDTO();
                    dto.setRule_id(rs.getInt("rule_id"));
                    dto.setName(rs.getString("name"));
                    dto.setCategory_id(rs.getInt("category_id"));
                    dto.setAmount(rs.getLong("amount"));
                    dto.setBase_date(rs.getString("base_date"));
                    dto.setP_value(rs.getInt("p_value"));
                    dto.setP_unit(rs.getString("p_unit"));
                    dto.setCategory_name(rs.getString("category_name"));
                    return dto;
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] 정기수입 규칙 단건 조회 실패: " + e.getMessage());
        }
        return null;
    }

    /**
     * 정기수입 규칙 등록.
     *
     * @param dto name, category_id, amount, base_date, p_value, p_unit이 채워진 DTO
     * @return DB가 자동 발급한 rule_id, 실패 시 -1
     */
    public int insertIncomeRule(RegularIncomeRuleDTO dto) {
        String sql = "INSERT INTO regular_income_rule (name, category_id, amount, base_date, p_value, p_unit) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, dto.getName());
            if (dto.getCategory_id() > 0) pstmt.setInt(2, dto.getCategory_id());
            else                          pstmt.setNull(2, Types.INTEGER);
            pstmt.setLong(3, dto.getAmount());
            pstmt.setDate(4, java.sql.Date.valueOf(dto.getBase_date()));
            pstmt.setInt(5, dto.getP_value());
            pstmt.setString(6, dto.getP_unit());
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] 정기수입 규칙 등록 실패: " + e.getMessage());
        }
        return -1;
    }

    /**
     * 정기수입 규칙 삭제.
     *
     * @param ruleId 삭제할 규칙 ID
     * @return 삭제 성공 시 true
     */
    public boolean deleteIncomeRule(int ruleId) {
        String sql = "DELETE FROM regular_income_rule WHERE rule_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, ruleId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ERROR] 정기수입 규칙 삭제 실패: " + e.getMessage());
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────
    //  정기수입 영수증 (regular_income_receipt)
    // ─────────────────────────────────────────────────────────

    /**
     * 정기수입 영수증 전체 목록 조회.
     *
     * @return RegularIncomeReceiptDTO 리스트 (최신 날짜순)
     */
    public List<RegularIncomeReceiptDTO> findAllIncomeReceipts() {
        List<RegularIncomeReceiptDTO> list = new ArrayList<>();
        String sql = "SELECT ir.ledger_id, lm.type_code, ir.rule_id, ir.asset_id, ir.category_id, " +
                     "ir.amount, DATE_FORMAT(ir.transaction_date, '%Y-%m-%d') AS transaction_date, " +
                     "lc.category_name, " +
                     "COALESCE(CONCAT(ab.bank_name, ' (', SUBSTR(at2.acc_number, -4), ')'), ca.name) AS asset_name, " +
                     "r.name AS rule_name " +
                     "FROM regular_income_receipt ir " +
                     "JOIN ledger_master lm ON ir.ledger_id = lm.ledger_id " +
                     "LEFT JOIN ledger_category lc ON ir.category_id = lc.category_id " +
                     "LEFT JOIN account_table at2 ON ir.asset_id = at2.asset_id " +
                     "LEFT JOIN account_bank ab ON at2.bank_id = ab.bank_id " +
                     "LEFT JOIN cash_asset ca ON ir.asset_id = ca.asset_id " +
                     "LEFT JOIN regular_income_rule r ON ir.rule_id = r.rule_id " +
                     "ORDER BY ir.transaction_date DESC, ir.ledger_id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                RegularIncomeReceiptDTO dto = new RegularIncomeReceiptDTO();
                dto.setLedger_id(rs.getInt("ledger_id"));
                dto.setType_code(rs.getString("type_code"));
                dto.setRule_id(rs.getInt("rule_id"));
                dto.setAsset_id(rs.getLong("asset_id"));
                dto.setCategory_id(rs.getInt("category_id"));
                dto.setAmount(rs.getLong("amount"));
                dto.setTransaction_date(rs.getString("transaction_date"));
                dto.setCategory_name(rs.getString("category_name"));
                dto.setAsset_name(rs.getString("asset_name"));
                dto.setRule_name(rs.getString("rule_name"));
                list.add(dto);
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] 정기수입 영수증 목록 조회 실패: " + e.getMessage());
        }
        return list;
    }

    /**
     * 정기수입 영수증 단건 조회 (삭제 전 잔액 복원 용도).
     *
     * @param ledgerId 조회할 내역 ID
     * @return RegularIncomeReceiptDTO (asset_id, amount만 채워짐), 없으면 null
     */
    public RegularIncomeReceiptDTO findIncomeReceiptById(int ledgerId) {
        String sql = "SELECT ledger_id, asset_id, amount FROM regular_income_receipt WHERE ledger_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, ledgerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    RegularIncomeReceiptDTO dto = new RegularIncomeReceiptDTO();
                    dto.setLedger_id(rs.getInt("ledger_id"));
                    dto.setAsset_id(rs.getLong("asset_id"));
                    dto.setAmount(rs.getLong("amount"));
                    return dto;
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] 정기수입 영수증 단건 조회 실패: " + e.getMessage());
        }
        return null;
    }

    /**
     * 정기수입 영수증 삽입.
     *
     * @param dto ledger_id, rule_id, asset_id, category_id, amount, transaction_date 채워진 DTO
     * @return 삽입 성공 시 true
     */
    public boolean insertIncomeReceipt(RegularIncomeReceiptDTO dto) {
        String sql = "INSERT INTO regular_income_receipt " +
                     "(ledger_id, asset_id, category_id, rule_id, amount, transaction_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, dto.getLedger_id());
            pstmt.setLong(2, dto.getAsset_id());
            if (dto.getCategory_id() > 0) pstmt.setInt(3, dto.getCategory_id());
            else                          pstmt.setNull(3, Types.INTEGER);
            pstmt.setInt(4, dto.getRule_id());
            pstmt.setLong(5, dto.getAmount());   // 수입이므로 양수
            pstmt.setDate(6, java.sql.Date.valueOf(dto.getTransaction_date()));
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ERROR] 정기수입 영수증 저장 실패: " + e.getMessage());
            return false;
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  변동지출 규칙 (variable_expense_rule)
    // ═══════════════════════════════════════════════════════════

    /**
     * 변동지출 규칙 전체 목록 조회.
     *
     * @return VariableExpenseRuleDTO 리스트
     */
    public List<VariableExpenseRuleDTO> findAllVariableRules() {
        List<VariableExpenseRuleDTO> list = new ArrayList<>();
        String sql = "SELECT r.rule_id, r.name, r.category_id, " +
                     "DATE_FORMAT(r.base_date, '%Y-%m-%d') AS base_date, " +
                     "r.p_value, r.p_unit, lc.category_name " +
                     "FROM variable_expense_rule r " +
                     "LEFT JOIN ledger_category lc ON r.category_id = lc.category_id " +
                     "ORDER BY r.rule_id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                VariableExpenseRuleDTO dto = new VariableExpenseRuleDTO();
                dto.setRule_id(rs.getInt("rule_id"));
                dto.setName(rs.getString("name"));
                dto.setCategory_id(rs.getInt("category_id"));
                dto.setBase_date(rs.getString("base_date"));
                dto.setP_value(rs.getInt("p_value"));
                dto.setP_unit(rs.getString("p_unit"));
                dto.setCategory_name(rs.getString("category_name"));
                list.add(dto);
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] 변동지출 규칙 목록 조회 실패: " + e.getMessage());
        }
        return list;
    }

    /**
     * 변동지출 규칙 단건 조회.
     *
     * @param ruleId 조회할 규칙 ID
     * @return VariableExpenseRuleDTO, 없으면 null
     */
    public VariableExpenseRuleDTO findVariableRuleById(int ruleId) {
        String sql = "SELECT r.rule_id, r.name, r.category_id, " +
                     "DATE_FORMAT(r.base_date, '%Y-%m-%d') AS base_date, " +
                     "r.p_value, r.p_unit, lc.category_name " +
                     "FROM variable_expense_rule r " +
                     "LEFT JOIN ledger_category lc ON r.category_id = lc.category_id " +
                     "WHERE r.rule_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, ruleId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    VariableExpenseRuleDTO dto = new VariableExpenseRuleDTO();
                    dto.setRule_id(rs.getInt("rule_id"));
                    dto.setName(rs.getString("name"));
                    dto.setCategory_id(rs.getInt("category_id"));
                    dto.setBase_date(rs.getString("base_date"));
                    dto.setP_value(rs.getInt("p_value"));
                    dto.setP_unit(rs.getString("p_unit"));
                    dto.setCategory_name(rs.getString("category_name"));
                    return dto;
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] 변동지출 규칙 단건 조회 실패: " + e.getMessage());
        }
        return null;
    }

    /**
     * 변동지출 규칙 등록.
     *
     * @param dto name, category_id, base_date, p_value, p_unit이 채워진 DTO (amount 없음)
     * @return DB가 자동 발급한 rule_id, 실패 시 -1
     */
    public int insertVariableRule(VariableExpenseRuleDTO dto) {
        String sql = "INSERT INTO variable_expense_rule (name, category_id, base_date, p_value, p_unit) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, dto.getName());
            if (dto.getCategory_id() > 0) pstmt.setInt(2, dto.getCategory_id());
            else                          pstmt.setNull(2, Types.INTEGER);
            pstmt.setDate(3, java.sql.Date.valueOf(dto.getBase_date()));
            pstmt.setInt(4, dto.getP_value());
            pstmt.setString(5, dto.getP_unit());
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] 변동지출 규칙 등록 실패: " + e.getMessage());
        }
        return -1;
    }

    /**
     * 변동지출 규칙 삭제.
     *
     * @param ruleId 삭제할 규칙 ID
     * @return 삭제 성공 시 true
     */
    public boolean deleteVariableRule(int ruleId) {
        String sql = "DELETE FROM variable_expense_rule WHERE rule_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, ruleId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ERROR] 변동지출 규칙 삭제 실패: " + e.getMessage());
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────
    //  변동지출 영수증 (variable_expense_receipt)
    // ─────────────────────────────────────────────────────────

    /**
     * 변동지출 영수증 전체 목록 조회.
     *
     * @return VariableExpenseReceiptDTO 리스트 (최신 날짜순)
     */
    public List<VariableExpenseReceiptDTO> findAllVariableReceipts() {
        List<VariableExpenseReceiptDTO> list = new ArrayList<>();
        String sql = "SELECT vr.ledger_id, lm.type_code, vr.rule_id, vr.asset_id, vr.category_id, " +
                     "vr.amount, DATE_FORMAT(vr.transaction_date, '%Y-%m-%d') AS transaction_date, " +
                     "vr.status, lc.category_name, " +
                     "COALESCE(CONCAT(ab.bank_name, ' (', SUBSTR(at2.acc_number, -4), ')'), ca.name) AS asset_name, " +
                     "r.name AS rule_name " +
                     "FROM variable_expense_receipt vr " +
                     "JOIN ledger_master lm ON vr.ledger_id = lm.ledger_id " +
                     "LEFT JOIN ledger_category lc ON vr.category_id = lc.category_id " +
                     "LEFT JOIN account_table at2 ON vr.asset_id = at2.asset_id " +
                     "LEFT JOIN account_bank ab ON at2.bank_id = ab.bank_id " +
                     "LEFT JOIN cash_asset ca ON vr.asset_id = ca.asset_id " +
                     "LEFT JOIN variable_expense_rule r ON vr.rule_id = r.rule_id " +
                     "ORDER BY vr.transaction_date DESC, vr.ledger_id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                VariableExpenseReceiptDTO dto = new VariableExpenseReceiptDTO();
                dto.setLedger_id(rs.getInt("ledger_id"));
                dto.setType_code(rs.getString("type_code"));
                dto.setRule_id(rs.getInt("rule_id"));
                dto.setAsset_id(rs.getLong("asset_id"));
                dto.setCategory_id(rs.getInt("category_id"));
                dto.setAmount(rs.getLong("amount"));
                dto.setTransaction_date(rs.getString("transaction_date"));
                dto.setStatus(rs.getString("status"));
                dto.setCategory_name(rs.getString("category_name"));
                dto.setAsset_name(rs.getString("asset_name"));
                dto.setRule_name(rs.getString("rule_name"));
                list.add(dto);
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] 변동지출 영수증 목록 조회 실패: " + e.getMessage());
        }
        return list;
    }

    /**
     * 변동지출 영수증 단건 조회 (삭제/확정 전 asset_id + amount + status 확인 용도).
     *
     * @param ledgerId 조회할 내역 ID
     * @return VariableExpenseReceiptDTO (asset_id, amount, status만 채워짐), 없으면 null
     */
    public VariableExpenseReceiptDTO findVariableReceiptById(int ledgerId) {
        String sql = "SELECT ledger_id, asset_id, amount, status FROM variable_expense_receipt WHERE ledger_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, ledgerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    VariableExpenseReceiptDTO dto = new VariableExpenseReceiptDTO();
                    dto.setLedger_id(rs.getInt("ledger_id"));
                    dto.setAsset_id(rs.getLong("asset_id"));
                    dto.setAmount(rs.getLong("amount"));
                    dto.setStatus(rs.getString("status"));
                    return dto;
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] 변동지출 영수증 단건 조회 실패: " + e.getMessage());
        }
        return null;
    }

    /**
     * 변동지출 영수증 삽입 (status='PENDING', amount=0).
     * 규칙 발동 시 PENDING 상태로만 생성됨. 실제 금액은 confirmVariableReceipt()에서 입력.
     *
     * @param dto ledger_id, rule_id, asset_id, category_id, transaction_date가 채워진 DTO
     * @return 삽입 성공 시 true
     */
    public boolean insertVariableReceipt(VariableExpenseReceiptDTO dto) {
        String sql = "INSERT INTO variable_expense_receipt " +
                     "(ledger_id, asset_id, category_id, rule_id, amount, transaction_date, status) " +
                     "VALUES (?, ?, ?, ?, 0, ?, 'PENDING')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, dto.getLedger_id());
            pstmt.setLong(2, dto.getAsset_id());
            if (dto.getCategory_id() > 0) pstmt.setInt(3, dto.getCategory_id());
            else                          pstmt.setNull(3, Types.INTEGER);
            pstmt.setInt(4, dto.getRule_id());
            pstmt.setDate(5, java.sql.Date.valueOf(dto.getTransaction_date()));
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ERROR] 변동지출 영수증(PENDING) 저장 실패: " + e.getMessage());
            return false;
        }
    }

    /**
     * 변동지출 영수증 확정: status를 'CONFIRMED'로, amount를 실제 금액(음수)으로 갱신.
     * 자산 잔액 차감은 Service에서 별도로 처리.
     *
     * @param ledgerId 확정할 영수증 ID
     * @param amount   실제 지출 금액 (항상 음수)
     * @return 갱신 성공 시 true
     */
    public boolean confirmVariableReceipt(int ledgerId, long amount) {
        String sql = "UPDATE variable_expense_receipt SET status = 'CONFIRMED', amount = ? WHERE ledger_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, amount);   // 음수 금액
            pstmt.setInt(2, ledgerId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ERROR] 변동지출 영수증 확정 실패: " + e.getMessage());
            return false;
        }
    }
}
