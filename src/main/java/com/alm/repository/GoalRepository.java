package com.alm.repository;

import com.alm.dto.GoalDTO;
import com.alm.util.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 재무 목표 DB 접근 계층.
 *
 * [설계 특이점]
 *   goal_table.asset_id 는 NULL 허용 (전체 자산 기준 목표).
 *   asset_master 에 ON DELETE SET NULL 설정 → 자산 삭제 시 목표는 남고 asset_id 가 NULL 로 전환.
 *
 * [asset_name 조회 — COALESCE + LEFT JOIN]
 *   자산 타입마다 이름 컬럼이 다르므로, 4개 자식 테이블을 모두 LEFT JOIN 후
 *   COALESCE 로 첫 번째 non-NULL 값을 선택한다.
 *   asset_id = NULL 이면 모두 NULL → Service 에서 "전체 자산 합계" 로 대체.
 *
 * [rs.wasNull() 패턴]
 *   getInt() 는 DB NULL 을 0 으로 반환한다.
 *   0 과 NULL 을 구분해야 하는 asset_id 컬럼은 getInt() 직후 rs.wasNull() 로 확인 후
 *   Integer nullable 필드에 null 또는 실제 값을 세팅한다.
 */
public class GoalRepository {

    // ── 목표 CRUD ─────────────────────────────────────────────────────

    public List<GoalDTO> findAllGoals() {
        List<GoalDTO> list = new ArrayList<>();
        String sql =
            "SELECT g.goal_id, g.asset_id, g.goal_name, g.target_amount, " +
            "DATE_FORMAT(g.end_date, '%Y-%m-%d') AS end_date, " +
            "COALESCE(" +
            "  CONCAT(ab.bank_name, ' (', SUBSTR(at2.acc_number, -4), ')'), " +
            "  ca.name, " +
            "  pa.item_name, " +
            "  CONCAT(re.contract_type, ' ', re.address) " +
            ") AS asset_name " +
            "FROM goal_table g " +
            "LEFT JOIN asset_master   am  ON g.asset_id = am.asset_id " +
            "LEFT JOIN account_table  at2 ON g.asset_id = at2.asset_id " +
            "LEFT JOIN account_bank   ab  ON at2.bank_id = ab.bank_id " +
            "LEFT JOIN cash_asset     ca  ON g.asset_id = ca.asset_id " +
            "LEFT JOIN physical_asset pa  ON g.asset_id = pa.asset_id " +
            "LEFT JOIN real_estate    re  ON g.asset_id = re.asset_id " +
            "ORDER BY g.goal_id DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                GoalDTO dto = new GoalDTO();
                dto.setGoal_id(rs.getInt("goal_id"));
                int assetId = rs.getInt("asset_id");
                dto.setAsset_id(rs.wasNull() ? null : assetId);
                dto.setGoal_name(rs.getString("goal_name"));
                dto.setTarget_amount(rs.getLong("target_amount"));
                dto.setEnd_date(rs.getString("end_date"));
                dto.setAsset_name(rs.getString("asset_name"));
                list.add(dto);
            }

        } catch (SQLException e) {
            System.err.println("[ERROR] 목표 목록 조회 실패: " + e.getMessage());
        }
        return list;
    }

    /** 단건 조회 (삭제 전 존재 여부 검증용). asset_name JOIN 생략. */
    public GoalDTO findGoalById(int goalId) {
        String sql =
            "SELECT goal_id, asset_id, goal_name, target_amount, " +
            "DATE_FORMAT(end_date, '%Y-%m-%d') AS end_date " +
            "FROM goal_table WHERE goal_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, goalId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    GoalDTO dto = new GoalDTO();
                    dto.setGoal_id(rs.getInt("goal_id"));
                    int assetId = rs.getInt("asset_id");
                    dto.setAsset_id(rs.wasNull() ? null : assetId);
                    dto.setGoal_name(rs.getString("goal_name"));
                    dto.setTarget_amount(rs.getLong("target_amount"));
                    dto.setEnd_date(rs.getString("end_date"));
                    return dto;
                }
            }

        } catch (SQLException e) {
            System.err.println("[ERROR] 목표 단건 조회 실패: " + e.getMessage());
        }
        return null;
    }

    /**
     * 목표 등록.
     * asset_id / end_date 는 NULL 허용 필드 — setNull() 로 바인딩.
     *
     * @return 생성된 goal_id, 실패 시 -1
     */
    public int insertGoal(GoalDTO dto) {
        String sql =
            "INSERT INTO goal_table (asset_id, goal_name, target_amount, end_date) " +
            "VALUES (?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            if (dto.getAsset_id() != null) pstmt.setInt(1, dto.getAsset_id());
            else                           pstmt.setNull(1, Types.INTEGER);

            pstmt.setString(2, dto.getGoal_name());
            pstmt.setLong(3, dto.getTarget_amount());

            if (dto.getEnd_date() != null && !dto.getEnd_date().isEmpty())
                pstmt.setDate(4, java.sql.Date.valueOf(dto.getEnd_date()));
            else
                pstmt.setNull(4, Types.DATE);

            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("[ERROR] 목표 등록 실패: " + e.getMessage());
        }
        return -1;
    }

    public boolean deleteGoal(int goalId) {
        String sql = "DELETE FROM goal_table WHERE goal_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, goalId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ERROR] 목표 삭제 실패: " + e.getMessage());
            return false;
        }
    }

    // ── 달성률 계산용 자산 가치 조회 ──────────────────────────────────

    /**
     * 특정 자산의 현재 가치 조회.
     *
     * [단일 쿼리로 4개 타입 처리]
     *   COALESCE(at2.balance, ca.balance, pa.current_value, re.price, 0)
     *   → 해당 자산 타입의 LEFT JOIN 만 성공하고 나머지는 NULL
     *   → COALESCE 가 첫 번째 non-NULL 값 선택. 마지막 0 은 미등록 상태 기본값.
     */
    public long getAssetCurrentValue(int assetId) {
        String sql =
            "SELECT COALESCE(at2.balance, ca.balance, pa.current_value, re.price, 0) AS val " +
            "FROM asset_master am " +
            "LEFT JOIN account_table  at2 ON am.asset_id = at2.asset_id " +
            "LEFT JOIN cash_asset     ca  ON am.asset_id = ca.asset_id " +
            "LEFT JOIN physical_asset pa  ON am.asset_id = pa.asset_id " +
            "LEFT JOIN real_estate    re  ON am.asset_id = re.asset_id " +
            "WHERE am.asset_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, assetId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getLong("val");
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] 자산 현재가치 조회 실패: " + e.getMessage());
        }
        return 0L;
    }

    /**
     * 전체 자산 합계 조회 (asset_id = NULL 인 목표용).
     * 4개 자식 테이블의 SUM 을 서브쿼리로 각각 구해 합산. 단 한 번의 DB 왕복.
     */
    public long getTotalAssetValue() {
        String sql =
            "SELECT " +
            "  COALESCE((SELECT SUM(balance)       FROM account_table),  0) + " +
            "  COALESCE((SELECT SUM(balance)       FROM cash_asset),     0) + " +
            "  COALESCE((SELECT SUM(current_value) FROM physical_asset), 0) + " +
            "  COALESCE((SELECT SUM(price)         FROM real_estate),    0) AS total";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) return rs.getLong("total");
        } catch (SQLException e) {
            System.err.println("[ERROR] 전체 자산 합계 조회 실패: " + e.getMessage());
        }
        return 0L;
    }

    /**
     * 목표 등록 모달용 자산 드롭다운 데이터 (ACC + CSH 만 허용).
     * REA / PHY 는 목표 달성 추적 대상으로 부적합해 제외.
     * LinkedHashMap 으로 JSON 키 순서 고정.
     */
    public List<Map<String, Object>> findAllAssetsForSelect() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql =
            "SELECT am.asset_id, am.type_code, " +
            "COALESCE(" +
            "  CONCAT(ab.bank_name, ' (', SUBSTR(at2.acc_number, -4), ')'), " +
            "  ca.name" +
            ") AS display_name, " +
            "COALESCE(at2.balance, ca.balance, 0) AS current_value " +
            "FROM asset_master am " +
            "LEFT JOIN account_table  at2 ON am.asset_id = at2.asset_id " +
            "LEFT JOIN account_bank   ab  ON at2.bank_id = ab.bank_id " +
            "LEFT JOIN cash_asset     ca  ON am.asset_id = ca.asset_id " +
            "WHERE am.type_code IN ('ACC', 'CSH') " +
            "ORDER BY am.asset_id";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("asset_id",      rs.getInt("asset_id"));
                row.put("type_code",     rs.getString("type_code"));
                row.put("display_name",  rs.getString("display_name"));
                row.put("current_value", rs.getLong("current_value"));
                list.add(row);
            }

        } catch (SQLException e) {
            System.err.println("[ERROR] 자산 목록(목표용) 조회 실패: " + e.getMessage());
        }
        return list;
    }

    /** 자산 삭제 가능 여부 판단용 — 해당 자산을 목표로 연동한 행 존재 여부. */
    public boolean existsByAssetId(long assetId) {
        String sql = "SELECT COUNT(*) FROM goal_table WHERE asset_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, assetId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] 목표 참조 여부 확인 실패: " + e.getMessage());
        }
        return false;
    }
}
