package com.alm.repository;

import com.alm.dto.UserSecurityDTO;
import com.alm.util.DBConnection;
import java.sql.*;

/**
 * [Repository 계층 - DAO]
 * user_security 테이블에 직접 접근하여 보안 관련 SQL을 실행하는 클래스.
 *
 * [테이블 구조]
 *   user_security:
 *     - password_hash VARCHAR(64): SHA-256 해시값 (64자리 16진수)
 *     - last_login_date DATETIME: 마지막 로그인 시각
 *   - 단일 행 테이블: 1인용 로컬 앱이므로 사용자 계정이 1개.
 *     INSERT 한 번 후 UPDATE만 사용. 두 번째 INSERT는 없음.
 *
 * [Java 문법 개념] try-with-resources (반복 학습):
 *   - 이 클래스의 모든 메서드에서 동일하게 사용됨.
 *   - try (Connection conn = ...; PreparedStatement pstmt = ...) { }
 *   - 블록 종료 시 pstmt.close() → conn.close() 순으로 자동 호출.
 *   - LIFO(Last In First Out) 순서로 close: 마지막에 열린 것이 먼저 닫힘.
 */
public class SecurityRepository {

    /**
     * 초기 비밀번호 해시값을 user_security 테이블에 저장.
     * 앱 생애주기 동안 단 한 번만 호출됨 (최초 설정 시).
     *
     * [Java 문법 개념] PreparedStatement와 SQL Injection 방지:
     *   - "INSERT INTO user_security ... VALUES (?, ?)":
     *     ? 플레이스홀더에 값을 setString(), setTimestamp()로 바인딩.
     *   - SQL Injection: 악의적인 사용자가 입력값에 SQL 코드를 삽입하여 DB를 조작하는 공격.
     *     예: 비밀번호에 "'; DROP TABLE user_security; --" 입력 → 테이블 삭제 시도.
     *   - PreparedStatement는 값을 SQL 구문이 아닌 데이터로 처리 → SQL Injection 원천 차단.
     *   - Statement (일반)와 달리 SQL과 파라미터를 분리하여 DB에 전달.
     *
     * [Java 문법 개념] pstmt.executeUpdate() > 0:
     *   - executeUpdate(): DML(INSERT, UPDATE, DELETE) 실행 후 영향받은 행 수(int) 반환.
     *   - > 0: 1행 이상 삽입/수정/삭제되면 true → 성공 판단.
     *
     * @param dto passwordHash, isFirstLogin, lastLoginDate를 담은 DTO
     * @return 저장 성공 시 true
     */
    public boolean saveInitialPassword(UserSecurityDTO dto) {
        String sql = "INSERT INTO user_security (password_hash, last_login_date) VALUES (?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, dto.getPasswordHash()); // 1번째 ? = password_hash
            // Timestamp.valueOf(LocalDateTime): LocalDateTime → java.sql.Timestamp 변환
            // getLastLoginDate()가 LocalDateTime 타입 → JDBC의 setTimestamp()는 Timestamp 요구
            pstmt.setTimestamp(2, Timestamp.valueOf(dto.getLastLoginDate())); // 2번째 ? = last_login_date

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace(); // 스택 트레이스를 콘솔에 출력 (디버깅용)
            return false;
        }
    }

    /**
     * DB에서 저장된 SHA-256 해시값을 조회하여 반환.
     * 로그인 인증 시 입력값 해시와 비교하는 데 사용.
     *
     * [Java 문법 개념] ResultSet 처리:
     *   - pstmt.executeQuery(): SELECT 실행 후 결과를 ResultSet으로 반환.
     *   - rs.next(): ResultSet 커서를 다음 행으로 이동. 데이터 있으면 true, 없으면 false.
     *   - LIMIT 1: DB가 첫 번째 행만 반환하도록 지시 → 불필요한 데이터 전송 방지.
     *
     * [Java 문법 개념] null 반환:
     *   - user_security에 데이터가 없으면 rs.next()가 false → null 반환.
     *   - 호출자(SecurityService.checkFirstLogin())에서 == null 체크로 최초 접속 여부 판단.
     *
     * @return 저장된 비밀번호 해시 문자열. DB에 데이터 없으면 null.
     */
    public String getPasswordHash() {
        String sql = "SELECT password_hash FROM user_security LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) { // executeQuery(): SELECT 전용

            if (rs.next()) return rs.getString("password_hash"); // 컬럼명으로 값 추출

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; // 데이터 없거나 예외 발생 시 null
    }

    /**
     * 로그인 성공 시 마지막 접속 시각(last_login_date)을 현재 시각으로 업데이트.
     *
     * [Java 문법 개념] WHERE 1=1:
     *   - "WHERE 1=1"은 항상 참인 조건 → 테이블의 모든 행에 UPDATE 적용.
     *   - user_security가 단일 행 테이블이므로 특정 PK 조건 없이 전체 갱신.
     *   - 다중 사용자 시스템에서는 반드시 "WHERE user_id = ?" 형태로 특정 사용자를 지정해야 함.
     *
     * @param lastLoginDate 갱신할 시각 (java.sql.Timestamp 타입)
     */
    public void updateLastLogin(Timestamp lastLoginDate) {
        String sql = "UPDATE user_security SET last_login_date = ? WHERE 1=1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setTimestamp(1, lastLoginDate);
            pstmt.executeUpdate(); // 반환값(영향받은 행 수) 미사용 → void 메서드

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
