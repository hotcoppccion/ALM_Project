package com.alm.util;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * KIS API 시장 지수 캐싱 스케줄러.
 *
 * [설계 의도]
 *   화면 로드 시마다 KIS API 를 직접 호출하면 TR_ID 당 초당 1건 제한에 걸린다.
 *   서버가 1초마다 한 번만 KIS 를 호출하고 결과를 메모리에 캐싱해
 *   사용자 수와 무관하게 서버 메모리에서 즉시 응답한다.
 *
 * [volatile]
 *   스케줄러 스레드(쓰기)와 HTTP 요청 스레드(읽기)가 동시에 접근하므로
 *   CPU 캐시 불일치 방지를 위해 volatile 선언.
 */
@Component
public class MarketDataScheduler {

    private static volatile Map<String, Object> cached = buildEmpty();
    private static volatile long lastUpdatedMs = 0;

    /**
     * 1초마다 코스피 지수 갱신. 실패 시 직전 캐시 값 유지.
     * initialDelay = 0 : 앱 시작 직후 즉시 첫 실행 (지연 없이 초기 데이터 확보).
     */
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

        // 나스닥/원달러: KIS 모의투자 서버가 해외 지수 API 미지원 — 실전 계좌 전환 후 추가 가능.

        cached = next;
        lastUpdatedMs = System.currentTimeMillis();
    }

    public static Map<String, Object> getCached()      { return cached; }
    public static long                getLastUpdatedMs() { return lastUpdatedMs; }

    private static Map<String, Object> indexMap(String value, String change, boolean error) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("value",  value);
        m.put("change", change);
        m.put("error",  error);
        return m;
    }

    /** 첫 @Scheduled 실행 전까지 보여줄 초기 빈 데이터. */
    private static Map<String, Object> buildEmpty() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("kospi", indexMap("--", "0", false));
        return m;
    }
}
