package com.alm.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 시스템 내의 보안 관련 암호화 및 해싱 기능을 담당하는 유틸리티 클래스
 */
public class SecurityUtil {

    /**
     * 사용자의 평문 비밀번호를 단방향 암호화(해싱)하는 메서드
     * SHA-256 알고리즘을 사용하여 원본 복원이 불가능한 64자리 16진수 문자열로 변환함
     *
     * @param plainText 사용자가 입력한 평문 비밀번호
     * @return 암호화된 SHA-256 해시 문자열
     */
    public static String encryptSHA256(String plainText) {
        try {
            // 1. 자바에서 제공하는 MessageDigest 에서 SHA-256 암호화 호출
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            // 2. 평문 문자열을 잘게 부수어 바이트(Byte) 단위로 기계에 집어넣음
            md.update(plainText.getBytes());

            // 3. 암호화된 바이트 배열)
            byte[] byteData = md.digest();

            // 4. 16진수(Hex) 문자열로
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < byteData.length; i++) {
                // 바이트 값을 양수화(& 0xff)하고 16진수로 변환한 뒤, 1자리수면 앞에 0을 붙임
                sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
            }

            // 5. 최종 완성된 64자리 암호 반환
            return sb.toString();

        } catch (NoSuchAlgorithmException e) {
            // 자바 버전에 따라 SHA-256 알고리즘이 지원되지 않을 경우의 치명적 예외 처리
            e.printStackTrace();
            throw new RuntimeException("보안 에러: 시스템이 SHA-256 암호화 알고리즘을 지원하지 않습니다.");
        }
    }
}