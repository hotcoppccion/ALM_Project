package com.alm.repository;

import com.alm.dto.UserSecurityDTO;
import com.alm.util.DBConnection;
import java.sql.*;

public class SecurityRepository {

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

    public String getPasswordHash() {
        String sql = "SELECT password_hash FROM user_security LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) return rs.getString("password_hash");
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public void updateLastLogin(Timestamp lastLoginDate) {
        String sql = "UPDATE user_security SET last_login_date = ? WHERE 1=1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setTimestamp(1, lastLoginDate);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}