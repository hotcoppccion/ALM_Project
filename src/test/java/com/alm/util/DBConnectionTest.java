package com.alm.util;

import java.sql.Connection;

public class DBConnectionTest {
    public static void main(String[] args) {
        System.out.println("=== JDBC 연결 테스트 시작 ===");

        // try-with-resources 구문을 사용하여 자동 자원 반납 확인
        try (Connection conn = DBConnection.getConnection()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("✅ [성공] 데이터베이스 연결에 성공하였습니다.");
                System.out.println("연결 정보: " + conn.getMetaData().getURL());
            }
        } catch (Exception e) {
            System.err.println("❌ [실패] 연결 중 오류 발생:");
            e.printStackTrace();
        }
    }
}