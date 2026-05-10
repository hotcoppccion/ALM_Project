package com.alm.service;

import com.alm.dto.UserSecurityDTO;
import com.alm.repository.SecurityRepository;
import com.alm.util.SecurityUtil;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class SecurityService {

    private final SecurityRepository repository = new SecurityRepository();

    /**
     * 최초 접속 상태를 확인
     */
    public boolean checkFirstLogin() {
        // DB에서 해시값
        String hash = repository.getPasswordHash();
        // 해시가 없으면(null) 한 번도 비번을 설정한 적 없는 '최초 접속'
        return hash == null;
    }

    /**
     * [로직 2] 사용자 인증 처리
     */
    public boolean authenticate(String inputPassword) {
        // 1. 입력받은 평문을 (SecurityUtil)에 넣음
        String inputHash = SecurityUtil.encryptSHA256(inputPassword);

        // 2. (Repository)에 저장된 해시를 꺼내옴 [cite: 267]
        String dbHash = repository.getPasswordHash();

        // 3. (해시)가 똑같은지 비교함
        if (inputHash != null && inputHash.equals(dbHash)) {
            // 일치하면 접속일 갱신
            repository.updateLastLogin(Timestamp.valueOf(LocalDateTime.now()));
            return true;
        }
        return false;
    }

    /**
     *  초기 비밀번호 설정
     */
    public boolean setupInitialPassword(String password) {
        //  평문을 암호화
        String hash = SecurityUtil.encryptSHA256(password);

        //  (DTO)'에 데이터를 담음
        // 비밀번호 해시, 최초로그인아님(false), 현재시간
        UserSecurityDTO dto = new UserSecurityDTO(hash, false, LocalDateTime.now());

        // 저장
        return repository.saveInitialPassword(dto);
    }
}