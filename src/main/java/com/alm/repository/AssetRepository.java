package com.alm.repository;

import com.alm.dto.*;
import com.alm.util.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AssetRepository {

    // ───────────────────────────── INSERT ─────────────────────────────

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

    // [수정] bank_id, type_id 파라미터 추가 + 테이블명 확인
    public boolean insertAccountDetails(AccountDTO dto, int bankId, int typeId) {
        String sql = "INSERT INTO account_table (asset_id, bank_id, type_id, acc_number, balance, account_interest) VALUES (?, ?, ?, ?, ?, ?)";
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

    // [수정] 테이블명: real_estate_table → real_estate
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

    // [수정] 테이블명: physical_asset_table → physical_asset
    public boolean insertPhysicalDetails(PhysicalAssetDTO dto) {
        String sql = "INSERT INTO physical_asset (asset_id, item_name, purchase_price, current_value, last_updated) VALUES (?, ?, ?, ?, NOW())";
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

    // [수정] 테이블명: cash_table → cash_asset
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

    // ───────────────────────────── UPDATE ─────────────────────────────

    public boolean updateAccountDetails(AccountDTO dto, int bankId, int typeId) {
        String sql = "UPDATE account_table SET bank_id=?, type_id=?, acc_number=?, balance=?, account_interest=? WHERE asset_id=?";
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
        String sql = "UPDATE physical_asset SET item_name=?, purchase_price=?, current_value=?, last_updated=NOW() WHERE asset_id=?";
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

    // ───────────────────────────── SELECT ALL ─────────────────────────────

    // [전면 재작성] 4개 타입을 각각 조회하여 올바른 DTO 반환
    public List<AssetDTO> findAllAssets() {
        List<AssetDTO> list = new ArrayList<>();

        // 1. 금융 계좌 (ACC)
        String accSql = "SELECT m.asset_id, m.type_code, a.acc_number, a.balance, a.account_interest, " +
                "a.bank_id, a.type_id, b.bank_name, t.type_name " +
                "FROM asset_master m " +
                "JOIN account_table a ON m.asset_id = a.asset_id " +
                "LEFT JOIN account_bank b ON a.bank_id = b.bank_id " +
                "LEFT JOIN account_type t ON a.type_id = t.type_id " +
                "WHERE m.type_code = 'ACC' ORDER BY m.created_at DESC";
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

        // 2. 부동산 (REA)
        String reaSql = "SELECT m.asset_id, m.type_code, r.contract_type, r.address, r.price " +
                "FROM asset_master m JOIN real_estate r ON m.asset_id = r.asset_id " +
                "WHERE m.type_code = 'REA' ORDER BY m.created_at DESC";
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

        // 3. 실물 자산 (PHY)
        String phySql = "SELECT m.asset_id, m.type_code, p.item_name, p.purchase_price, p.current_value " +
                "FROM asset_master m JOIN physical_asset p ON m.asset_id = p.asset_id " +
                "WHERE m.type_code = 'PHY' ORDER BY m.created_at DESC";
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

        // 4. 현금 (CSH)
        String cshSql = "SELECT m.asset_id, m.type_code, c.name, c.balance " +
                "FROM asset_master m JOIN cash_asset c ON m.asset_id = c.asset_id " +
                "WHERE m.type_code = 'CSH' ORDER BY m.created_at DESC";
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

    // ───────────────────────────── SELECT ONE (수정 모달용) ─────────────────────────────

    public AssetDTO findById(long assetId, String typeCode) {
        switch (typeCode) {
            case "ACC": {
                String sql = "SELECT m.asset_id, m.type_code, a.acc_number, a.balance, a.account_interest, " +
                        "a.bank_id, a.type_id, b.bank_name, t.type_name " +
                        "FROM asset_master m JOIN account_table a ON m.asset_id = a.asset_id " +
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
                } catch (SQLException e) { System.err.println("[ERROR] ACC 단건 조회: " + e.getMessage()); }
                break;
            }
            case "REA": {
                String sql = "SELECT m.asset_id, r.contract_type, r.address, r.price " +
                        "FROM asset_master m JOIN real_estate r ON m.asset_id = r.asset_id WHERE m.asset_id = ?";
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
                } catch (SQLException e) { System.err.println("[ERROR] REA 단건 조회: " + e.getMessage()); }
                break;
            }
            case "PHY": {
                String sql = "SELECT m.asset_id, p.item_name, p.purchase_price, p.current_value " +
                        "FROM asset_master m JOIN physical_asset p ON m.asset_id = p.asset_id WHERE m.asset_id = ?";
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
                } catch (SQLException e) { System.err.println("[ERROR] PHY 단건 조회: " + e.getMessage()); }
                break;
            }
            case "CSH": {
                String sql = "SELECT m.asset_id, c.name, c.balance " +
                        "FROM asset_master m JOIN cash_asset c ON m.asset_id = c.asset_id WHERE m.asset_id = ?";
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
                } catch (SQLException e) { System.err.println("[ERROR] CSH 단건 조회: " + e.getMessage()); }
                break;
            }
        }
        return null;
    }

    // ───────────────────────────── DELETE ─────────────────────────────

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

    // ───────────────────────────── 마스터 데이터 (드롭다운용) ─────────────────────────────

    public List<Map<String, Object>> findAllBanks() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT bank_id, bank_name FROM account_bank ORDER BY bank_id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("bank_id", rs.getInt("bank_id"));
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
                row.put("type_id", rs.getInt("type_id"));
                row.put("type_name", rs.getString("type_name"));
                list.add(row);
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] 계좌종류 목록 조회 실패: " + e.getMessage());
        }
        return list;
    }
}
