package com.alm.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * SHA-256 단방향 해시 유틸리티.
 *
 * [한계] 솔트(Salt) 미적용 — 동일 비밀번호가 항상 동일 해시.
 * 실무 전환 시 BCrypt / Argon2 로 교체 권장.
 */
public class SecurityUtil {

    /**
     * 평문 비밀번호를 SHA-256 해시 후 64자리 16진수 문자열 반환.
     * @throws RuntimeException 정상 JVM 환경에서는 발생하지 않음 (SHA-256 은 Java 표준 스펙 필수 지원)
     */
    public static String encryptSHA256(String plainText) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(plainText.getBytes());
            byte[] byteData = md.digest();

            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < byteData.length; i++) {
                sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
            }
            return sb.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new RuntimeException("보안 에러: 시스템이 SHA-256을 지원하지 않습니다.");
        }
    }
}
