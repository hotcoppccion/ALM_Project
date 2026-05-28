package com.alm.repository;

import com.alm.dto.UserSecurityDTO;
import com.alm.util.DBConnection;
import java.sql.*;

/**
 * user_security 테이블 DB 접근 계층.
 *
 * [테이블 특성]
 *   1인용 로컬 앱 — 단일 행 테이블.
 *   최초 설정 시 INSERT 한 번, 이후 로그인마다 last_login_date UPDATE.
 */
public class SecurityRepository {

    /** 최초 비밀번호 해시 저장. 앱 생애주기 중 단 한 번만 호출. */
    public boolean saveInitialPassword(UserSecurityDTO dto) {
        String sql = "INSERT INTO user_security (password_hash, last_login_date) VALUES (?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, dto.getPasswordHash());
            pstmt.setTimestamp(2, Timestamp.valueOf(dto.getLastLoginDate()));
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /** 저장된 SHA-256 해시 반환. DB 에 데이터 없으면 null (= 최초 접속). */
    public String getPasswordHash() {
        String sql = "SELECT password_hash FROM user_security LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) return rs.getString("password_hash");

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 로그인 성공 시 접속 시각 갱신.
     * WHERE 1=1 : 단일 행 테이블이므로 전체 갱신.
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
