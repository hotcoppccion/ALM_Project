package com.alm.util;

import java.io.InputStream;
import java.util.Properties;

/**
 * application.properties 설정값 로더.
 * DB 비밀번호, KIS API 키 등 코드에 직접 하드코딩하면 안 되는 값을 외부 파일에서 읽는다.
 * 클래스 로드 시 딱 한 번 파일을 읽고 이후 요청은 메모리에서 반환.
 */
public class ConfigUtil {

    private static final Properties properties = new Properties();

    static {
        try (InputStream input = ConfigUtil.class
                .getClassLoader()
                .getResourceAsStream("application.properties")) {

            if (input == null) {
                System.out.println("[설정 오류] application.properties 파일을 찾을 수 없습니다.");
            } else {
                properties.load(input);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }
}
