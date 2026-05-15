package com.alm.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * [Util 계층 - DB 연결 관리]
 * 프로그램 전체에서 단일 진입점으로 DB Connection을 생성하고 반환하는 유틸리티 클래스.
 *
 * [Java 문법 개념] static 메서드:
 *   - 클래스 인스턴스 없이 클래스명.메서드명()으로 직접 호출 가능.
 *   - DBConnection.getConnection(): 매번 new DBConnection() 없이 호출.
 *   - 상태(인스턴스 필드)를 가지지 않는 순수 기능 제공 클래스에 static 메서드가 적합.
 *
 * [설계 결정] Connection-per-Request 방식:
 *   - 매 DB 작업 호출 시마다 새 Connection 객체를 생성하여 반환.
 *   - Connection Pool(HikariCP 등) 미사용: 학습 목적의 단순 구조 유지.
 *   - 장점: 구현 단순. 단점: 매번 TCP 핸드셰이크 비용 발생 → 실무에서는 Pool 사용 권장.
 *   - Repository에서 try-with-resources로 사용 후 자동 close() → 연결 누수 없음.
 */
public class DBConnection {

    /**
     * 외부 설정 파일(application.properties)에서 DB 접속 정보를 읽어 Connection 객체를 생성.
     *
     * [Java 문법 개념] throws SQLException:
     *   - 이 메서드는 SQLException(checked exception)을 던질 수 있음을 선언.
     *   - 호출자(Repository 메서드)가 try-catch 또는 throws로 반드시 처리해야 함.
     *   - Repository에서 try (Connection conn = DBConnection.getConnection())으로 처리.
     *
     * @return java.sql.Connection: JDBC DB 연결 객체. 이 객체로 SQL 실행 가능.
     * @throws SQLException DB 접속 정보 불일치, MySQL 서버 미실행, 네트워크 오류 시 발생.
     */
    public static Connection getConnection() throws SQLException {

        // ConfigUtil.getProperty(): application.properties 파일에서 key에 해당하는 값을 읽어옴.
        // 하드코딩 방지: URL, 계정, 비밀번호를 소스 코드에 직접 쓰지 않음 → 보안성 향상.
        // 예: db.url=jdbc:mysql://localhost:3306/alm_db?serverTimezone=Asia/Seoul
        String url      = ConfigUtil.getProperty("db.url");
        String user     = ConfigUtil.getProperty("db.user");
        String password = ConfigUtil.getProperty("db.password");

        try {
            // [Java 문법 개념] Class.forName(className):
            //   - 런타임에 문자열로 지정한 클래스를 동적으로 로드하는 리플렉션(Reflection) API.
            //   - MySQL JDBC 드라이버 클래스를 JVM에 로드하여 DriverManager가 인식하게 함.
            //   - MySQL 8.0 이상: com.mysql.cj.jdbc.Driver (구버전 com.mysql.jdbc.Driver와 구분).
            //   - MySQL Connector/J 8.0+에서는 자동 로드를 지원하지만, 명시적으로 선언하여 안정성 확보.
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            // ClassNotFoundException: 지정한 클래스가 classpath에 없을 때 발생.
            // build.gradle.kts의 implementation("com.mysql:mysql-connector-j:8.3.0")가 누락된 경우.
            throw new SQLException("JDBC 드라이버 로드 실패: " + e.getMessage());
        }

        // [Java 문법 개념] DriverManager.getConnection(url, user, password):
        //   - JDBC 표준 API. 등록된 드라이버 중 url 형식에 맞는 드라이버로 연결 시도.
        //   - url 형식: jdbc:mysql://호스트:포트/DB명?파라미터
        //     예: jdbc:mysql://localhost:3306/alm_db?serverTimezone=Asia/Seoul
        //   - serverTimezone=Asia/Seoul: MySQL의 DATETIME을 한국 시간(KST, UTC+9)으로 해석.
        //   - 반환된 Connection은 사용 후 반드시 close() 해야 함 (try-with-resources 권장).
        return DriverManager.getConnection(url, user, password);
    }
}
