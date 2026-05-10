package com.alm.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DBConnection.class
 * 외부 설정 파일(properties)을 로드하여 데이터베이스 연결을 관리하는 Utility 클래스.
 */
public class DBConnection {

    /**
     * ConfigUtil을 통해 설정 정보를 읽어와 독립적인 Connection 객체를 반환함.
     * @return java.sql.Connection
     * @throws SQLException DB 접속 정보 불일치 또는 네트워크 오류 시 발생
     */
    public static Connection getConnection() throws SQLException {
        // 하드코딩 없이 ConfigUtil(또는 환경 변수)에서 정보를 동적으로 로드함
        String url = ConfigUtil.getProperty("db.url");
        String user = ConfigUtil.getProperty("db.user");
        String password = ConfigUtil.getProperty("db.password");

        try {
            // JDBC 드라이버 로드 (MySQL 8.0 이상 cj 드라이버 사용)
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("❌ JDBC 드라이버 로드 실패: " + e.getMessage());
        }

        // 호출 시마다 새로운 인스턴스를 반환하여 Thread-safety 및 자원 격리 확보
        return DriverManager.getConnection(url, user, password);
    }
}