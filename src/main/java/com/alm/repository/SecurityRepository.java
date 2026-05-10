package com.alm.repository;

import com.alm.dto.UserSecurityDTO;
import com.alm.util.DBConnection;
import java.sql.*;

/**
 * 5.1-3) SecurityRepository.class
 * Persistence Layer: 데이터베이스 물리 계층에 직접 접근하여 자원 생명주기를 관리함.
 */
public class SecurityRepository {

    /**
     * @param dto 암호화된 데이터를 포함한 객체
     * @return INSERT 실행 결과 성공 여부
     */
    public boolean saveInitialPassword(UserSecurityDTO dto) {
        String sql = "INSERT INTO user_security (password_hash, is_first_login, last_login_date) VALUES (?, ?, ?)";

        // [중요] 타입을 반드시 java.sql.Connection으로 명시하여 AutoCloseable을 활성화함.
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, dto.getPasswordHash());
            pstmt.setBoolean(2, dto.isFirstLogin());
            pstmt.setTimestamp(3, Timestamp.valueOf(dto.getLastLoginDate()));

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * @return DB에 저장된 SHA-256 해시값
     */
    public String getPasswordHash() {
        String sql = "SELECT password_hash FROM user_security LIMIT 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                return rs.getString("password_hash");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 사용자 인증 성공 시 최종 접근 시각을 갱신함.
     * @param lastLoginDate 시스템 현재 시각의 Timestamp
     */
    public void updateLastLogin(Timestamp lastLoginDate) {
        String sql = "UPDATE user_security SET last_login_date = ? WHERE 1=1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setTimestamp(1, lastLoginDate);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}