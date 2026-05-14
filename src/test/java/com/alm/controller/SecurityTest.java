
package com.alm.controller;

import com.alm.dto.UserSecurityDTO;
import com.alm.repository.SecurityRepository;
import java.time.LocalDateTime;

public class SecurityTest {
    public static void main(String[] args) {
        SecurityRepository repo = new SecurityRepository();

        // 1. 가짜 DTO 생성
        UserSecurityDTO testDto = new UserSecurityDTO("test_hash_1234", false, LocalDateTime.now());

        // 2. 저장 시도
        System.out.println("=== DB 저장 테스트 시작 ===");
        boolean result = repo.saveInitialPassword(testDto);

        if (result) {
            System.out.println("✅ 성공: DB에 데이터가 정상적으로 박혔습니다.");
        } else {
            System.out.println("❌ 실패: SQL 에러나 DB 연결 문제를 확인하세요.");
        }

        // 3. 조회 테스트
        String hash = repo.getPasswordHash();
        System.out.println("조회된 해시값: " + hash);
    }
}