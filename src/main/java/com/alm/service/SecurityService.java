package com.alm.service;

import com.alm.dto.UserSecurityDTO;
import com.alm.repository.SecurityRepository;
import com.alm.util.SecurityUtil;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * [Service 계층 - 보안 비즈니스 로직]
 * 사용자 인증, 최초 접속 판단, 비밀번호 초기 설정 등의 핵심 보안 로직을 수행하는 클래스.
 *
 * [인증 흐름]
 *   1. 사용자가 비밀번호 입력
 *   2. SecurityService.authenticate(inputPassword) 호출
 *   3. SecurityUtil.encryptSHA256(inputPassword) → 입력값 해시
 *   4. SecurityRepository.getPasswordHash() → DB 저장 해시 조회
 *   5. 두 해시값 비교 → 일치하면 true 반환
 *
 * [보안 설계]
 *   - 비밀번호 평문 비교 없음. DB에 평문 저장 없음.
 *   - 해시값만 DB에 저장 → DB가 유출되어도 비밀번호 원본 복원 불가.
 */
public class SecurityService {

    // Repository: DB 접근 전담
    private final SecurityRepository repository = new SecurityRepository();

    // SecurityUtil: SHA-256 해시 처리 담당. static 메서드만 있지만 인스턴스로 사용.
    // [Java 문법 개념] static 메서드를 인스턴스 참조로 호출:
    //   - util.encryptSHA256()처럼 인스턴스로도 호출 가능하나, SecurityUtil.encryptSHA256()이 더 명확.
    //   - 컴파일러가 static으로 처리하므로 성능 차이 없음. 인스턴스 생성 자체도 무해하지만 불필요.
    private final SecurityUtil util = new SecurityUtil();

    /**
     * DB에 비밀번호 해시가 존재하는지 확인하여 최초 접속 여부를 판단.
     *
     * [Java 문법 개념] 반환값 활용:
     *   - repository.getPasswordHash()가 null이면 DB에 데이터 없음 → 최초 접속 → true 반환.
     *   - == null: 참조 비교. null은 "객체 없음"을 나타내는 Java 특수 리터럴.
     *
     * @return DB에 비밀번호 없으면 true(최초 접속), 있으면 false(재접속)
     */
    public boolean checkFirstLogin() {
        return repository.getPasswordHash() == null;
    }

    /**
     * 입력 비밀번호와 DB 저장 해시값을 비교하여 인증 수행.
     * 인증 성공 시 최종 접속 시각(last_login_date)을 DB에 업데이트.
     *
     * [Java 문법 개념] String.equals():
     *   - String 내용 비교 메서드. == 연산자는 참조(메모리 주소) 비교이므로 String 값 비교엔 사용 불가.
     *   - inputHash.equals(dbHash): inputHash가 null이면 NPE 발생.
     *     → inputHash != null 조건을 앞에 두어 NPE 방지 (단락 평가, Short-circuit Evaluation).
     *
     * [Java 문법 개념] 단락 평가(Short-circuit Evaluation):
     *   - && (AND) 연산: 앞 조건이 false이면 뒤 조건을 평가하지 않음.
     *   - inputHash != null && inputHash.equals(dbHash):
     *     inputHash가 null이면 && 앞 조건이 false → .equals() 호출 안 됨 → NPE 방지.
     *
     * [Java 문법 개념] java.time.LocalDateTime:
     *   - Java 8에서 도입된 날짜+시간 표현 클래스 (java.util.Date의 대체).
     *   - LocalDateTime.now(): 현재 시스템의 로컬 날짜와 시간을 가져옴.
     *
     * [Java 문법 개념] Timestamp.valueOf(LocalDateTime):
     *   - java.sql.Timestamp: JDBC에서 SQL DATETIME/TIMESTAMP 컬럼과 매핑되는 타입.
     *   - LocalDateTime → Timestamp 변환이 필요한 이유: JDBC PreparedStatement.setTimestamp()는 Timestamp를 요구.
     *
     * @param inputPassword 사용자가 로그인 폼에 입력한 평문 비밀번호
     * @return 인증 성공 시 true, 실패 시 false
     */
    public boolean authenticate(String inputPassword) {
        String inputHash = util.encryptSHA256(inputPassword); // 입력 비밀번호를 SHA-256 해시
        String dbHash    = repository.getPasswordHash();      // DB에서 저장된 해시 조회

        if (inputHash != null && inputHash.equals(dbHash)) {
            // 로그인 성공 시 접속 시각 갱신: LocalDateTime.now() → Timestamp → DB UPDATE
            repository.updateLastLogin(Timestamp.valueOf(LocalDateTime.now()));
            return true;
        }
        return false;
    }

    /**
     * 최초 비밀번호 설정: 입력 비밀번호를 해시 후 DB에 저장.
     *
     * [Java 문법 개념] UserSecurityDTO 생성자 호출:
     *   - new UserSecurityDTO(hash, false, LocalDateTime.now()):
     *     인자 3개를 받는 생성자(Parameterized Constructor)를 호출.
     *   - isFirstLogin=false: 비밀번호 설정 완료 → 더 이상 최초 접속 상태가 아님.
     *
     * @param password 사용자가 설정하려는 평문 비밀번호
     * @return DB 저장 성공 시 true
     */
    public boolean setupInitialPassword(String password) {
        String hash = util.encryptSHA256(password); // 설정 비밀번호를 SHA-256 해시

        // DTO에 해시값, 최초접속 여부, 현재 시각을 담아 Repository로 전달
        UserSecurityDTO dto = new UserSecurityDTO(hash, false, LocalDateTime.now());
        return repository.saveInitialPassword(dto);
    }
}
