package com.alm.util;

/**
 * 타입 안전 파싱 헬퍼 모음.
 *
 * JSON 역직렬화 시 숫자가 Integer / Long / String 등 다양한 타입으로 넘어올 수 있으므로
 * toString() 후 파싱하는 방식으로 타입을 통일한다.
 * AssetService / LedgerService / GoalService 등 여러 곳에 중복됐던 파싱 로직을 한 곳으로 모았다.
 * 상태가 없는 순수 유틸리티 클래스이므로 모든 메서드를 static 으로 선언한다.
 */
public class ParseUtil {

    private ParseUtil() {}

    /** Object → long. null / 빈값 / 파싱 불가 시 0L 반환. */
    public static long parseLong(Object obj) {
        if (obj == null) return 0L;
        String s = obj.toString().trim();
        if (s.isEmpty() || "null".equals(s)) return 0L;
        try { return Long.parseLong(s); } catch (NumberFormatException e) { return 0L; }
    }

    /** Object → int. 파싱 실패 시 defaultVal 반환. */
    public static int parseInt(Object obj, int defaultVal) {
        if (obj == null) return defaultVal;
        String s = obj.toString().trim();
        if (s.isEmpty() || "null".equals(s)) return defaultVal;
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return defaultVal; }
    }

    /** Object → double. 이자율·수익률 등 소수점 값에 사용. */
    public static double parseDouble(Object obj) {
        if (obj == null) return 0.0;
        String s = obj.toString().trim();
        if (s.isEmpty() || "null".equals(s)) return 0.0;
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return 0.0; }
    }
}
