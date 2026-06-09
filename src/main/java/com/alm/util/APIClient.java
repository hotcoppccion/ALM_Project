package com.alm.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;

/**
 * KIS OpenAPI 클라이언트. 토큰 발급·캐싱, 국내/해외 현재가, 코스피 지수 조회를 제공한다.
 * 6자리 숫자 티커 → 국내(KOSPI/KOSDAQ), 그 외 → 해외(NASDAQ 기본값).
 * 해외 가격은 달러 단위로 반환되며 원화 환산은 미구현.
 */
public class APIClient {

    private static final String APP_KEY    = ConfigUtil.getProperty("kis.app-key");
    private static final String APP_SECRET = ConfigUtil.getProperty("kis.app-secret");
    private static final String BASE_URL;
    static {
        String url = ConfigUtil.getProperty("kis.base-url");
        BASE_URL = (url != null && !url.isBlank()) ? url.trim()
                 : "https://openapivts.koreainvestment.com:9443";
    }

    private static final HttpClient   HTTP = HttpClient.newHttpClient();
    private static final ObjectMapper OM   = new ObjectMapper();

    // 토큰 캐시 (클래스 레벨 — 앱 전체에서 재사용)
    private static String  accessToken;
    private static Instant tokenExpiry;

    // ── 토큰 발급 ─────────────────────────────────────────────────────

    /** 토큰 반환. 캐시 유효 시 재사용, 만료 5분 전 자동 재발급. synchronized 로 중복 발급 방지. */
    private static synchronized String getToken() throws Exception {
        if (accessToken != null && tokenExpiry != null
                && Instant.now().isBefore(tokenExpiry)) {
            return accessToken;
        }

        String body = "{"
                + "\"grant_type\":\"client_credentials\","
                + "\"appkey\":\""    + APP_KEY    + "\","
                + "\"appsecret\":\"" + APP_SECRET + "\""
                + "}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/oauth2/tokenP"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        JsonNode json = OM.readTree(res.body());

        if (!json.has("access_token"))
            throw new Exception("KIS 토큰 발급 실패: " + res.body());

        accessToken = json.get("access_token").asText();
        long expiresIn = json.has("expires_in") ? json.get("expires_in").asLong() : 86400L;
        tokenExpiry = Instant.now().plusSeconds(expiresIn - 300); // 만료 5분 전 갱신

        System.out.println("[KIS] 토큰 발급 완료. 만료 예정: " + tokenExpiry);
        return accessToken;
    }

    // ── 종목 현재가 (국내/해외 자동 감지) ────────────────────────────

    /**
     * @return String[] { 현재가, 전일대비등락률(%), 종목명, 시장구분 }
     * @throws Exception 네트워크 또는 API 오류
     */
    public static String[] getStockInfo(String tickerCode) throws Exception {
        boolean isDomestic = tickerCode != null && tickerCode.matches("\\d{6}");
        return isDomestic ? getDomesticStockInfo(tickerCode) : getOverseasStockInfo(tickerCode);
    }

    // ── 국내 주식 현재가 ─────────────────────────────────────────────

    /**
     * KOSPI/KOSDAQ 현재가. TR_ID: FHKST01010100.
     * custtype: "P" (개인) 누락 시 output 이 빈 객체로 반환되므로 필수.
     */
    private static String[] getDomesticStockInfo(String tickerCode) throws Exception {
        String token = getToken();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL
                        + "/uapi/domestic-stock/v1/quotations/inquire-price"
                        + "?FID_COND_MRKT_DIV_CODE=J&FID_INPUT_ISCD=" + tickerCode))
                .header("authorization", "Bearer " + token)
                .header("appkey",    APP_KEY)
                .header("appsecret", APP_SECRET)
                .header("tr_id",     "FHKST01010100")
                .header("custtype",  "P")
                .header("tr_cont",   "")
                .GET().build();

        HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        JsonNode json = OM.readTree(res.body());

        String rtCd = json.path("rt_cd").asText("");
        if (!"0".equals(rtCd)) {
            System.err.println("[KIS 국내] 오류 응답: " + res.body());
            throw new Exception("KIS 국내 API 오류: " + json.path("msg1").asText(res.body()));
        }

        JsonNode out = json.path("output");
        String price      = out.path("stck_prpr").asText("0");
        String changeRate = out.path("prdy_ctrt").asText("0");
        String stockName  = out.path("hts_kor_isnm").asText("");
        String marketRaw  = out.path("rprs_mrkt_kor_name").asText("");
        String marketType = marketRaw.contains("코스닥") ? "KOSDAQ" : "KOSPI";

        return new String[]{ price, changeRate, stockName, marketType };
    }

    // ── 해외 주식 현재가 ─────────────────────────────────────────────

    /**
     * NASDAQ/NYSE/AMEX 현재가. TR_ID: HHDFS76200200.
     * 기본 거래소: NAS(NASDAQ). 실전 서비스에서는 사용자 선택으로 확장 필요.
     * 가격은 달러 단위 소수점 문자열 → 정수 부분만 반환.
     */
    private static String[] getOverseasStockInfo(String tickerCode) throws Exception {
        String token = getToken();
        String excd  = "NAS"; // 기본값 NASDAQ

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL
                        + "/uapi/overseas-price/v1/quotations/price"
                        + "?AUTH=&EXCD=" + excd + "&SYMB=" + tickerCode))
                .header("authorization", "Bearer " + token)
                .header("appkey",    APP_KEY)
                .header("appsecret", APP_SECRET)
                .header("tr_id",     "HHDFS76200200")
                .header("custtype",  "P")
                .header("tr_cont",   "")
                .GET().build();

        HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        JsonNode json = OM.readTree(res.body());

        String rtCd = json.path("rt_cd").asText("");
        if (!"0".equals(rtCd)) {
            System.err.println("[KIS 해외] 오류 응답: " + res.body());
            throw new Exception("KIS 해외 API 오류: " + json.path("msg1").asText(res.body()));
        }

        JsonNode out = json.path("output");
        String priceRaw   = out.path("last").asText("0");
        String changeRate = out.path("diff").asText("0");
        String stockName  = out.path("name").asText("");

        // 소수점 이하 제거: "185.72" → "185"
        String priceInt = priceRaw.contains(".")
                ? priceRaw.substring(0, priceRaw.indexOf('.'))
                : priceRaw;

        String marketType = excd.equals("NAS") ? "NASDAQ"
                          : excd.equals("NYS") ? "NYSE"
                          : excd.equals("AMS") ? "AMEX"
                          : excd;

        return new String[]{ priceInt, changeRate, stockName, marketType };
    }


    // ── 코스피 지수 조회 ──────────────────────────────────────────────

    /**
     * 코스피 지수 현재값. TR_ID: FHPUP02100000, FID_INPUT_ISCD=0001.
     * @return String[] { 현재값, 전일대비등락률(%) }
     */
    public static String[] getKospiIndex() throws Exception {
        String token = getToken();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL
                        + "/uapi/domestic-stock/v1/quotations/inquire-index-price"
                        + "?FID_COND_MRKT_DIV_CODE=U&FID_INPUT_ISCD=0001"))
                .header("authorization", "Bearer " + token)
                .header("appkey",    APP_KEY)
                .header("appsecret", APP_SECRET)
                .header("tr_id",     "FHPUP02100000")
                .header("custtype",  "P")
                .GET().build();

        HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        JsonNode json = OM.readTree(res.body());
        String rtCd = json.path("rt_cd").asText("");
        if (!"0".equals(rtCd)) {
            System.err.println("[KIS 코스피] 오류: " + res.body());
            throw new Exception("KOSPI 조회 실패: " + json.path("msg1").asText(res.body()));
        }
        JsonNode out = json.path("output");
        return new String[]{
            out.path("bstp_nmix_prpr").asText("0"),
            out.path("bstp_nmix_prdy_ctrt").asText("0")
        };
    }
}
