package com.alm.test;

import com.alm.service.SecurityService;

public class ManualTest {
    public static void main(String[] args) {
        // 백엔드 전체 흐름(Service -> Repository -> DB)을 한 번에 테스트합니다.
        SecurityService service = new SecurityService();

        System.out.println("=== 백엔드 단독 테스트 시작 ===");

        // 1. 가짜 비밀번호로 저장 시도
        String testPassword = "test1234!";
        boolean isSaved = service.setupInitialPassword(testPassword);

        if (isSaved) {
            System.out.println("✅ 결과: DB 저장에 성공했습니다!");

            // 2. 저장된 비번으로 로그인(검증) 테스트
            boolean isAuth = service.authenticate(testPassword);
            System.out.println("✅ 검증 결과: " + (isAuth ? "일치함" : "불일치함"));

        } else {
            System.out.println("❌ 결과: DB 저장 실패. SQL이나 DB 연결을 확인하세요.");
        }
    }
}