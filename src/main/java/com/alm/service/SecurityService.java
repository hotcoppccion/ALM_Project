package com.alm.service;

import com.alm.dto.UserSecurityDTO;
import com.alm.repository.SecurityRepository;
import com.alm.util.SecurityUtil;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * 보안 서비스.
 * 입력 비밀번호를 SHA-256 해시 후 DB 저장 값과 비교한다. 평문은 저장하지 않는다.
 */
public class SecurityService {

    private final SecurityRepository repository = new SecurityRepository();
    private final SecurityUtil        util       = new SecurityUtil();

    /** DB 에 password_hash 가 없으면 true (최초 접속). */
    public boolean checkFirstLogin() {
        return repository.getPasswordHash() == null;
    }

    /**
     * 입력 비밀번호 해시와 DB 저장 해시 비교. 성공 시 접속 시각 갱신.
     * @return 인증 성공 true, 실패 false
     */
    public boolean authenticate(String inputPassword) {
        String inputHash = util.encryptSHA256(inputPassword);
        String dbHash    = repository.getPasswordHash();

        if (inputHash != null && inputHash.equals(dbHash)) {
            repository.updateLastLogin(Timestamp.valueOf(LocalDateTime.now()));
            return true;
        }
        return false;
    }

    /** 최초 비밀번호를 SHA-256 해시 후 DB 저장. */
    public boolean setupInitialPassword(String password) {
        String hash = util.encryptSHA256(password);
        UserSecurityDTO dto = new UserSecurityDTO(hash, false, LocalDateTime.now());
        return repository.saveInitialPassword(dto);
    }
}
