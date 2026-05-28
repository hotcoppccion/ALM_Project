package com.alm.dto;

import java.time.LocalDateTime;

/**
 * user_security 테이블 매핑 DTO. 보안 계층(Service ↔ Repository) 간 데이터 전달 전용.
 */
public class UserSecurityDTO {

    private String        passwordHash;    // SHA-256 해시된 비밀번호
    private boolean       isFirstLogin;    // 최초 접속 여부 플래그
    private LocalDateTime lastLoginDate;   // 마지막 로그인 타임스탬프

    public UserSecurityDTO() {}

    public UserSecurityDTO(String passwordHash, boolean isFirstLogin, LocalDateTime lastLoginDate) {
        this.passwordHash  = passwordHash;
        this.isFirstLogin  = isFirstLogin;
        this.lastLoginDate = lastLoginDate;
    }

    public String        getPasswordHash()                        { return passwordHash; }
    public void          setPasswordHash(String passwordHash)     { this.passwordHash = passwordHash; }

    public boolean       isFirstLogin()                           { return isFirstLogin; }
    public void          setFirstLogin(boolean firstLogin)        { isFirstLogin = firstLogin; }

    public LocalDateTime getLastLoginDate()                       { return lastLoginDate; }
    public void          setLastLoginDate(LocalDateTime v)        { this.lastLoginDate = v; }
}
