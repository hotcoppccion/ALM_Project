package com.alm.repository;

import com.alm.dto.AccountDTO;
import com.alm.util.DBConnection;
import java.sql.*;

/**
 * [Persistence Layer] 자산 도메인의 데이터 영속성을 전담하는 리포지토리 클래스입니다.
 * 마스터 식별자 생성 및 이자율 기반 계좌 조회 기능을 구현합니다.
 */
public class AssetRepository {

    /**
     * 자산의 부모 테이블인 asset_master에 레코드를 생성하고 자동 발급된 PK를 반환합니다.
     * @param typeCode 자산의 유형 코드 (예: 'ACC', 'CSH')
     * @return 데이터베이스에서 자동 생성된 전역 자산 식별자 (asset_id), 오류 시 -1 반환
     */
    public long insertAssetMaster(String typeCode) {
        // 전역 정수형 ID 발급을 위한 마스터 테이블 삽입 질의어 (동혁 님 원본 100% 유지)
        String sql = "INSERT INTO asset_master (type_code, created_at) VALUES (?, NOW())";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, typeCode);
            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] 자산 마스터 식별자 생성 중 예외 발생: " + e.getMessage());
        }
        return -1;
    }

    /**
     * 금융 계좌 고유의 이자율 정보와 함께 은행명, 통장 종류를 조인하여 상세 데이터를 조회합니다.
     * @param assetId 조회 대상이 되는 자산 식별자
     * @return 이자율 및 조인 데이터가 매핑된 AccountDTO 객체, 데이터 부재 시 null 반환
     */
    public AccountDTO findAccountWithInterest(long assetId) {
        // [수정 포인트] 동혁 님의 기존 원본 쿼리에 bank_name, type_name 두 컬럼만 LEFT JOIN으로 추가
        String sql = "SELECT a.*, b.bank_name, t.type_name " +
                "FROM account_table a " +
                "LEFT JOIN account_bank b ON a.bank_id = b.bank_id " +
                "LEFT JOIN account_type t ON a.type_id = t.type_id " +
                "WHERE a.asset_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, assetId); // 동혁 님 원본 파라미터 그대로 유지

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    AccountDTO dto = new AccountDTO();
                    dto.setAsset_id(rs.getLong("asset_id"));
                    dto.setAcc_number(rs.getString("acc_number"));
                    dto.setBalance(rs.getLong("balance"));
                    dto.setAccount_interest(rs.getDouble("account_interest"));

                    // 새로 추가된 두 컬럼 매핑
                    dto.setBank_name(rs.getString("bank_name"));
                    dto.setType_name(rs.getString("type_name"));

                    return dto;
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] 이자율 포함 계좌 정보 조회 중 예외 발생: " + e.getMessage());
        }
        return null;
    }
}