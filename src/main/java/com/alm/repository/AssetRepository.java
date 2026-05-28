package com.alm.repository;

import com.alm.dto.*;
import com.alm.util.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 자산 도메인 DB 접근 계층.
 *
 * [DB 구조 — Class Table Inheritance]
 *   asset_master (공통: asset_id, type_code, created_at)
 *       ├── account_table  — 금융계좌
 *       ├── real_estate    — 부동산
 *       ├── physical_asset — 실물자산
 *       └── cash_asset     — 현금성 자산
 *
 *   저장: asset_master INSERT 후 발급된 asset_id 로 자식 테이블 INSERT.
 *   삭제: asset_master DELETE 만으로 충분 — 자식 테이블에 ON DELETE CASCADE 적용.
 */
public class AssetRepository {

    // ── INSERT ────────────────────────────────────────────────────────

    /**
     * asset_master 에 레코드를 삽입하고 AUTO_INCREMENT asset_id 를 반환.
     * 자식 테이블 INSERT 전에 반드시 먼저 호출해야 한다.
     *
     * @return 발급된 asset_id, 실패 시 -1
     */
    public long insertAssetMaster(String typeCode) {
        String sql = "INSERT INTO asset_master (type_code, created_at) VALUES (?, NOW())";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, typeCode);
            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }

        } catch (SQLException e) {
            System.err.println("[ERROR] 마스터 생성 실패: " + e.getMessage());
        }
        return -1;
    }

    public boolean insertAccountDetails(AccountDTO dto, int bankId, int typeId) {
        String sql = "INSERT INTO account_table (asset_id, bank_id, type_id, acc_number, balance, account_interest) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, dto.getAsset_id());
            pstmt.setInt(2, bankId);
            pstmt.setInt(3, typeId);
            pstmt.setString(4, dto.getAcc_number());
            pstmt.setLong(5, dto.getBalance());
            pstmt.setDouble(6, dto.getAccount_interest());
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[ERROR] 계좌 저장 실패: " + e.getMessage());
            return false;
        }
    }

    public boolean insertRealEstateDetails(RealEstateDTO dto) {
        String sql = "INSERT INTO real_estate (asset_id, contract_type, address, price) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, dto.getAsset_id());
            pstmt.setString(2, dto.getContract_type());
            pstmt.setString(3, dto.getAddress());
            pstmt.setLong(4, dto.getPrice());
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[ERROR] 부동산 저장 실패: " + e.getMessage());
            return false;
        }
    }

    public boolean insertPhysicalDetails(PhysicalAssetDTO dto) {
        String sql = "INSERT INTO physical_asset (asset_id, item_name, purchase_price, current_value, last_updated) " +
                     "VALUES (?, ?, ?, ?, NOW())";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, dto.getAsset_id());
            pstmt.setString(2, dto.getItem_name());
            pstmt.setLong(3, dto.getPurchase_price());
            pstmt.setLong(4, dto.getCurrent_value());
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[ERROR] 실물자산 저장 실패: " + e.getMessage());
            return false;
        }
    }

    public boolean insertCashDetails(CashAssetDTO dto) {
        String sql = "INSERT INTO cash_asset (asset_id, name, balance) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, dto.getAsset_id());
            pstmt.setString(2, dto.getName());
            pstmt.setLong(3, dto.getBalance());
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[ERROR] 현금자산 저장 실패: " + e.getMessage());
            return false;
        }
    }

    // ── UPDATE ────────────────────────────────────────────────────────

    public boolean updateAccountDetails(AccountDTO dto, int bankId, int typeId) {
        String sql = "UPDATE account_table " +
                     "SET bank_id=?, type_id=?, acc_number=?, balance=?, account_interest=? " +
                     "WHERE asset_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, bankId);
            pstmt.setInt(2, typeId);
            pstmt.setString(3, dto.getAcc_number());
            pstmt.setLong(4, dto.getBalance());
            pstmt.setDouble(5, dto.getAccount_interest());
            pstmt.setLong(6, dto.getAsset_id());
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[ERROR] 계좌 수정 실패: " + e.getMessage());
            return false;
        }
    }

    public boolean updateRealEstateDetails(RealEstateDTO dto) {
        String sql = "UPDATE real_estate SET contract_type=?, address=?, price=? WHERE asset_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, dto.getContract_type());
            pstmt.setString(2, dto.getAddress());
            pstmt.setLong(3, dto.getPrice());
            pstmt.setLong(4, dto.getAsset_id());
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[ERROR] 부동산 수정 실패: " + e.getMessage());
            return false;
        }
    }

    public boolean updatePhysicalDetails(PhysicalAssetDTO dto) {
        String sql = "UPDATE physical_asset " +
                     "SET item_name=?, purchase_price=?, current_value=?, last_updated=NOW() " +
                     "WHERE asset_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, dto.getItem_name());
            pstmt.setLong(2, dto.getPurchase_price());
            pstmt.setLong(3, dto.getCurrent_value());
            pstmt.setLong(4, dto.getAsset_id());
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[ERROR] 실물자산 수정 실패: " + e.getMessage());
            return false;
        }
    }

    public boolean updateCashDetails(CashAssetDTO dto) {
        String sql = "UPDATE cash_asset SET name=?, balance=? WHERE asset_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, dto.getName());
            pstmt.setLong(2, dto.getBalance());
            pstmt.setLong(3, dto.getAsset_id());
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[ERROR] 현금자산 수정 실패: " + e.getMessage());
            return false;
        }
    }

    // ── SELECT ALL ────────────────────────────────────────────────────

    /**
     * 전체 자산 목록 반환 (4가지 타입을 각각 조회 후 하나의 List 로 합산).
     *
     * [타입별 쿼리 분리 이유]
     *   UNION 으로 합치면 타입마다 다른 컬럼을 NULL 로 채운 억지 구조가 된다.
     *   각 타입별 쿼리를 독립 실행하면 타입에 맞는 DTO 로 깔끔히 매핑되고,
     *   한 타입에서 오류가 나도 나머지 타입 조회는 계속 진행된다.
     *
     * [ACC 쿼리 LEFT JOIN]
     *   bank_id / type_id 가 NULL 인 계좌도 목록에 포함되어야 하므로 LEFT JOIN 사용.
     */
    public List<AssetDTO> findAllAssets() {
        List<AssetDTO> list = new ArrayList<>();

        // ACC
        String accSql =
                "SELECT m.asset_id, m.type_code, a.acc_number, a.balance, a.account_interest, " +
                "       a.bank_id, a.type_id, b.bank_name, t.type_name " +
                "FROM asset_master m " +
                "JOIN account_table a ON m.asset_id = a.asset_id " +
                "LEFT JOIN account_bank b ON a.bank_id = b.bank_id " +
                "LEFT JOIN account_type t ON a.type_id = t.type_id " +
                "WHERE m.type_code = 'ACC' " +
                "ORDER BY m.created_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(accSql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                AccountDTO dto = new AccountDTO();
                dto.setAsset_id(rs.getLong("asset_id"));
                dto.setType_code(rs.getString("type_code"));
                dto.setAcc_number(rs.getString("acc_number"));
                dto.setBalance(rs.getLong("balance"));
                dto.setAccount_interest(rs.getDouble("account_interest"));
                dto.setBank_id(rs.getInt("bank_id"));
                dto.setType_id(rs.getInt("type_id"));
                dto.setBank_name(rs.getString("bank_name"));
                dto.setType_name(rs.getString("type_name"));
                list.add(dto);
            }

        } catch (SQLException e) {
            System.err.println("[ERROR] ACC 조회 실패: " + e.getMessage());
        }

        // REA
        String reaSql =
                "SELECT m.asset_id, m.type_code, r.contract_type, r.address, r.price " +
                "FROM asset_master m " +
                "JOIN real_estate r ON m.asset_id = r.asset_id " +
                "WHERE m.type_code = 'REA' " +
                "ORDER BY m.created_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(reaSql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                RealEstateDTO dto = new RealEstateDTO();
                dto.setAsset_id(rs.getLong("asset_id"));
                dto.setType_code(rs.getString("type_code"));
                dto.setContract_type(rs.getString("contract_type"));
                dto.setAddress(rs.getString("address"));
                dto.setPrice(rs.getLong("price"));
                list.add(dto);
            }

        } catch (SQLException e) {
            System.err.println("[ERROR] REA 조회 실패: " + e.getMessage());
        }

        // PHY
        String phySql =
                "SELECT m.asset_id, m.type_code, p.item_name, p.purchase_price, p.current_value " +
                "FROM asset_master m " +
                "JOIN physical_asset p ON m.asset_id = p.asset_id " +
                "WHERE m.type_code = 'PHY' " +
                "ORDER BY m.created_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(phySql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                PhysicalAssetDTO dto = new PhysicalAssetDTO();
                dto.setAsset_id(rs.getLong("asset_id"));
                dto.setType_code(rs.getString("type_code"));
                dto.setItem_name(rs.getString("item_name"));
                dto.setPurchase_price(rs.getLong("purchase_price"));
                dto.setCurrent_value(rs.getLong("current_value"));
                list.add(dto);
            }

        } catch (SQLException e) {
            System.err.println("[ERROR] PHY 조회 실패: " + e.getMessage());
        }

        // CSH
        String cshSql =
                "SELECT m.asset_id, m.type_code, c.name, c.balance " +
                "FROM asset_master m " +
                "JOIN cash_asset c ON m.asset_id = c.asset_id " +
                "WHERE m.type_code = 'CSH' " +
                "ORDER BY m.created_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(cshSql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                CashAssetDTO dto = new CashAssetDTO();
                dto.setAsset_id(rs.getLong("asset_id"));
                dto.setType_code(rs.getString("type_code"));
                dto.setName(rs.getString("name"));
                dto.setBalance(rs.getLong("balance"));
                list.add(dto);
            }

        } catch (SQLException e) {
            System.err.println("[ERROR] CSH 조회 실패: " + e.getMessage());
        }

        return list;
    }

    // ── SELECT ONE ────────────────────────────────────────────────────

    /** typeCode 에 따라 자식 테이블을 조회해 해당 DTO 로 반환. 없으면 null. */
    public AssetDTO findById(long assetId, String typeCode) {

        switch (typeCode) {

            case "ACC": {
                String sql =
                        "SELECT m.asset_id, m.type_code, a.acc_number, a.balance, a.account_interest, " +
                        "       a.bank_id, a.type_id, b.bank_name, t.type_name " +
                        "FROM asset_master m " +
                        "JOIN account_table a ON m.asset_id = a.asset_id " +
                        "LEFT JOIN account_bank b ON a.bank_id = b.bank_id " +
                        "LEFT JOIN account_type t ON a.type_id = t.type_id " +
                        "WHERE m.asset_id = ?";

                try (Connection conn = DBConnection.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {

                    pstmt.setLong(1, assetId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            AccountDTO dto = new AccountDTO();
                            dto.setAsset_id(rs.getLong("asset_id"));
                            dto.setType_code("ACC");
                            dto.setAcc_number(rs.getString("acc_number"));
                            dto.setBalance(rs.getLong("balance"));
                            dto.setAccount_interest(rs.getDouble("account_interest"));
                            dto.setBank_id(rs.getInt("bank_id"));
                            dto.setType_id(rs.getInt("type_id"));
                            dto.setBank_name(rs.getString("bank_name"));
                            dto.setType_name(rs.getString("type_name"));
                            return dto;
                        }
                    }

                } catch (SQLException e) {
                    System.err.println("[ERROR] ACC 단건 조회: " + e.getMessage());
                }
                break;
            }

            case "REA": {
                String sql =
                        "SELECT m.asset_id, r.contract_type, r.address, r.price " +
                        "FROM asset_master m " +
                        "JOIN real_estate r ON m.asset_id = r.asset_id " +
                        "WHERE m.asset_id = ?";

                try (Connection conn = DBConnection.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {

                    pstmt.setLong(1, assetId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            RealEstateDTO dto = new RealEstateDTO();
                            dto.setAsset_id(rs.getLong("asset_id"));
                            dto.setType_code("REA");
                            dto.setContract_type(rs.getString("contract_type"));
                            dto.setAddress(rs.getString("address"));
                            dto.setPrice(rs.getLong("price"));
                            return dto;
                        }
                    }

                } catch (SQLException e) {
                    System.err.println("[ERROR] REA 단건 조회: " + e.getMessage());
                }
                break;
            }

            case "PHY": {
                String sql =
                        "SELECT m.asset_id, p.item_name, p.purchase_price, p.current_value " +
                        "FROM asset_master m " +
                        "JOIN physical_asset p ON m.asset_id = p.asset_id " +
                        "WHERE m.asset_id = ?";

                try (Connection conn = DBConnection.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {

                    pstmt.setLong(1, assetId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            PhysicalAssetDTO dto = new PhysicalAssetDTO();
                            dto.setAsset_id(rs.getLong("asset_id"));
                            dto.setType_code("PHY");
                            dto.setItem_name(rs.getString("item_name"));
                            dto.setPurchase_price(rs.getLong("purchase_price"));
                            dto.setCurrent_value(rs.getLong("current_value"));
                            return dto;
                        }
                    }

                } catch (SQLException e) {
                    System.err.println("[ERROR] PHY 단건 조회: " + e.getMessage());
                }
                break;
            }

            case "CSH": {
                String sql =
                        "SELECT m.asset_id, c.name, c.balance " +
                        "FROM asset_master m " +
                        "JOIN cash_asset c ON m.asset_id = c.asset_id " +
                        "WHERE m.asset_id = ?";

                try (Connection conn = DBConnection.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {

                    pstmt.setLong(1, assetId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            CashAssetDTO dto = new CashAssetDTO();
                            dto.setAsset_id(rs.getLong("asset_id"));
                            dto.setType_code("CSH");
                            dto.setName(rs.getString("name"));
                            dto.setBalance(rs.getLong("balance"));
                            return dto;
                        }
                    }

                } catch (SQLException e) {
                    System.err.println("[ERROR] CSH 단건 조회: " + e.getMessage());
                }
                break;
            }
        }

        return null;
    }

    // ── DELETE ────────────────────────────────────────────────────────

    /** asset_master 삭제. 자식 테이블은 ON DELETE CASCADE 로 DB 가 자동 삭제. */
    public boolean deleteById(long assetId) {
        String sql = "DELETE FROM asset_master WHERE asset_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, assetId);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[ERROR] 삭제 실패: " + e.getMessage());
            return false;
        }
    }

    // ── 마스터 데이터 (드롭다운용) ────────────────────────────────────

    /**
     * 은행 목록 반환.
     *
     * [InvestRepository 연동 주의]
     *   InvestRepository 에서 type_name = '증권위탁계좌' 로 계좌를 조회하므로
     *   account_type 씨드 데이터의 이름과 정확히 일치해야 한다.
     */
    public List<Map<String, Object>> findAllBanks() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT bank_id, bank_name FROM account_bank ORDER BY bank_id";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("bank_id",   rs.getInt("bank_id"));
                row.put("bank_name", rs.getString("bank_name"));
                list.add(row);
            }

        } catch (SQLException e) {
            System.err.println("[ERROR] 은행 목록 조회 실패: " + e.getMessage());
        }

        return list;
    }

    public List<Map<String, Object>> findAllAccountTypes() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT type_id, type_name FROM account_type ORDER BY type_id";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("type_id",   rs.getInt("type_id"));
                row.put("type_name", rs.getString("type_name"));
                list.add(row);
            }

        } catch (SQLException e) {
            System.err.println("[ERROR] 계좌종류 목록 조회 실패: " + e.getMessage());
        }

        return list;
    }

    /** @return 발급된 bank_id, 실패 시 -1 */
    public int insertBank(String bankName) {
        String sql = "INSERT INTO account_bank (bank_name) VALUES (?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, bankName);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] 은행 추가 실패: " + e.getMessage());
        }
        return -1;
    }

    /** @return 발급된 type_id, 실패 시 -1 */
    public int insertAccountType(String typeName) {
        String sql = "INSERT INTO account_type (type_name) VALUES (?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, typeName);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] 계좌종류 추가 실패: " + e.getMessage());
        }
        return -1;
    }
}
