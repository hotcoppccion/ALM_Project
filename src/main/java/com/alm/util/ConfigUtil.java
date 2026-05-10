package com.alm.util;

import java.io.InputStream; //application.properties라는 파일을 자바 프로그램 안으로 가져오는 도구
import java.util.Properties; //application.properties 파일 내용을 담아두는 바구니

public class ConfigUtil {
    private static final Properties properties = new Properties();

    static {
        //  getResourceAsStream: resources 폴더에 만든 'application.properties' 파일을 찾음
        try (InputStream input = ConfigUtil.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                System.out.println("❌ 에러: application.properties 파일을 찾을 수 없습니다.");
            } else {
                //load: 찾은 파일의 내용(비밀번호 등)을 properties 에 저장?
                properties.load(input);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } //예외방지
    }

    //  getProperty: 다른 클래스가 비밀번호 요청할 때 바구니에서 값을 꺼내주는 메서드
    public static String getProperty(String key) {
        return properties.getProperty(key);
    }
}