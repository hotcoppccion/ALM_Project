package com.alm.controller;

/**
 * 보안 모듈 통합 테스트 (Integration Test)
 */
public class SecurityTest {
    public static void main(String[] args) {
        SecurityController controller = new SecurityController();

        System.out.println("=== [테스트 1] 초기 비밀번호 설정 시작 ===");
        // 기획서 5.1-1-setupPassword() 테스트
        controller.setupPassword("donghyuk2026");

        System.out.println("\n=== [테스트 2] 로그인 인증 시도 ===");
        // 기획서 5.1-1-login() 테스트
        controller.login("donghyuk2026"); // 성공 케이스
        controller.login("wrongpassword"); // 실패 케이스
    }
}