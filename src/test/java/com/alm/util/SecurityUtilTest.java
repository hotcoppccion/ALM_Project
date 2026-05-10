package com.alm.util;

public class SecurityUtilTest {
    public static void main(String[] args) {
        String myPass = "1234"; // 내가 정한 비밀번호
        String encrypted = SecurityUtil.encryptSHA256(myPass);

        System.out.println("원래 비밀번호: " + myPass);
        System.out.println("암호화된 비밀번호: " + encrypted);
        System.out.println("암호 길이: " + encrypted.length());
    }
}