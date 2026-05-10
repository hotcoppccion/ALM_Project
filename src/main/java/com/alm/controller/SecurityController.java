package com.alm.controller;

import com.alm.service.SecurityService;

/**
 * 5.1-1) SecurityController.class
 * Presentation Layer에서 사용자의 인증 요청을 수신하고 비즈니스 로직 분기 및 View 제어를 수행함 [cite: 254-255].
 */
public class SecurityController {

    /**
     * Business Logic Layer와의 연동을 위한 Service 객체 의존성 주입(Dependency Injection).
     */
    private final SecurityService securityService = new SecurityService();

    /**
     * 초기 마스터 비밀번호 설정을 위한 Handler Method.
     * @param inputPassword Client로부터 전달된 Plain Text 형식의 패스워드 데이터
     */
    public void setupPassword(String inputPassword) {
        // Input Validation: 데이터 무결성을 위한 최소 길이 및 Null 여부 검증
        if (inputPassword == null || inputPassword.trim().isEmpty()) {
            System.out.println("❌ 오류: 입력값이 비어 있거나 유효하지 않습니다.");
            return;
        }

        // Service 계층으로 초기 비밀번호 영속화 로직 위임(Delegation)
        boolean success = securityService.setupInitialPassword(inputPassword);

        // 결과 상태에 따른 View 피드백 제어
        if (success) {
            System.out.println("✅ [성공] 마스터 비밀번호 저장이 완료되었습니다.");
        } else {
            System.out.println("❌ [실패] 시스템 런타임 오류: 저장이 실패했습니다.");
        }
    }

    /**
     * 사용자 인증 및 세션 제어를 위한 login 메소드[cite: 256].
     * @param inputPassword 인증을 위해 입력된 사용자 패스워드
     */
    public void login(String inputPassword) {
        // SecurityService의 authenticate 로직을 호출하여 인증 유효성 검증 [cite: 262]
        boolean isAuthenticated = securityService.authenticate(inputPassword);

        if (isAuthenticated) {
            System.out.println("🔓 [인증 성공] 접근이 허용되었습니다.");
        } else {
            System.out.println("🔒 [인증 실패] 비밀번호가 일치하지 않습니다.");
        }
    }
}