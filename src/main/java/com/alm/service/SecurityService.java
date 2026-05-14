package com.alm.service;

import com.alm.dto.UserSecurityDTO;
import com.alm.repository.SecurityRepository;
import com.alm.util.SecurityUtil;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class SecurityService {

    private final SecurityRepository repository = new SecurityRepository();
    // SecurityUtil의 메서드를 사용하기 위해 인스턴스 생성
    private final SecurityUtil util = new SecurityUtil();

    /**
     * 최초 접속 여부 확인
     * 판단 기준: DB에 패스워드 해시 데이터 존재 여부
     */
    public boolean checkFirstLogin() {
        return repository.getPasswordHash() == null;
    }

    /**
     * 사용자 인증 (로그인)
     */
    public boolean authenticate(String inputPassword) {
        // 어제 구현한 SHA-256 암호화 로직 사용
        String inputHash = util.encryptSHA256(inputPassword);
        String dbHash = repository.getPasswordHash();

        if (inputHash != null && inputHash.equals(dbHash)) {
            // 로그인 성공 시 최종 접속 시각 업데이트 (기존 Repository 인터페이스 준수)
            repository.updateLastLogin(Timestamp.valueOf(LocalDateTime.now()));
            return true;
        }
        return false;
    }

    /**
     * 초기 비밀번호 설정 및 저장
     */
    public boolean setupInitialPassword(String password) {
        String hash = util.encryptSHA256(password);

        // 동혁 님이 주신 DTO 생성자 형식: (hash, isFirstLogin, now)
        // 설정이 완료되는 시점이므로 isFirstLogin은 false로 전달
        UserSecurityDTO dto = new UserSecurityDTO(hash, false, LocalDateTime.now());

        // 기존 Repository의 saveInitialPassword(dto) 호출
        return repository.saveInitialPassword(dto);
    }
}