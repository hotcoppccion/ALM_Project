package com.alm.dto;

import java.time.LocalDateTime;

/**
 * 보안 및 인증 데이터를 계층(Layer) 간에 전송하기 위한 순수 데이터 객체(Data Transfer Object).
 * 데이터베이스의 user_security 테이블 엔티티(Entity)와 1:1로 매핑되는 속성들을 포함합니다.
 */
public class UserSecurityDTO {

    // 1. 멤버 변수 (Fields) : 데이터를 외부에서 함부로 조작하지 못하도록 private 접근 제어자 사용 (캡슐화)
    private String passwordHash;      // 단방향 암호화된 마스터 비밀번호 해시값
    private boolean isFirstLogin;     // 시스템 최초 접속 여부 플래그 (true/false)
    private LocalDateTime lastLoginDate; // 최종 인증(로그인) 완료 타임스탬프

    // 2. 기본 생성자 (Default Constructor) : 프레임워크나 라이브러리(MyBatis, Hibernate 등) 연동을 위해 필수
    public UserSecurityDTO() {
    }

    // 3. 모든 속성을 초기화하는 오버로딩 생성자 (All-Arguments Constructor)
    public UserSecurityDTO(String passwordHash, boolean isFirstLogin, LocalDateTime lastLoginDate) {
        this.passwordHash = passwordHash;
        this.isFirstLogin = isFirstLogin;
        this.lastLoginDate = lastLoginDate;
    }

    // 4. Getter & Setter 메서드 (무결성을 유지하며 데이터에 접근하기 위한 인터페이스)

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean isFirstLogin() {
        return isFirstLogin;
    }

    public void setFirstLogin(boolean firstLogin) {
        isFirstLogin = firstLogin;
    }

    public LocalDateTime getLastLoginDate() {
        return lastLoginDate;
    }

    public void setLastLoginDate(LocalDateTime lastLoginDate) {
        this.lastLoginDate = lastLoginDate;
    }
}