package com.alm.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * MySQL DB 연결 유틸리티.
 * 접속 정보는 application.properties 에서 읽어 코드와 설정을 분리한다.
 */
public class DBConnection {

    public static Connection getConnection() throws SQLException {
        String url      = ConfigUtil.getProperty("db.url");
        String user     = ConfigUtil.getProperty("db.user");
        String password = ConfigUtil.getProperty("db.password");

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL 드라이버를 찾을 수 없습니다: " + e.getMessage());
        }

        return DriverManager.getConnection(url, user, password);
    }
}
