package com.alm.util;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * KIS API 시장 지수 캐싱 스케줄러.
 * KIS API 초당 1건 제한 대응: 1초마다 서버에서 한 번만 호출하고 메모리에 캐싱.
 * cached 필드는 스케줄러 스레드(쓰기)와 요청 스레드(읽기)가 동시 접근하므로 volatile 선언.
 */
@Component
public class MarketDataScheduler {

    private static volatile Map<String, Object> cached = buildEmpty();
    private static volatile long lastUpdatedMs = 0;

    @Scheduled(fixedRate = 1000, initialDelay = 0)
    public void refresh() {
        Map<String, Object> next = new LinkedHashMap<>();

        try {
            String[] kospi = APIClient.getKospiIndex();
            next.put("kospi", indexMap(kospi[0], kospi[1], false));
        } catch (Exception e) {
            System.err.println("[SCHEDULER] 코스피 조회 실패: " + e.getMessage());
            Object prev = cached.get("kospi");
            next.put("kospi", prev != null ? prev : indexMap("--", "0", true));
        }

        cached = next;
        lastUpdatedMs = System.currentTimeMillis();
    }

    public static Map<String, Object> getCached()        { return cached; }
    public static long                getLastUpdatedMs() { return lastUpdatedMs; }

    private static Map<String, Object> indexMap(String value, String change, boolean error) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("value",  value);
        m.put("change", change);
        m.put("error",  error);
        return m;
    }

    private static Map<String, Object> buildEmpty() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("kospi", indexMap("--", "0", false));
        return m;
    }
}
