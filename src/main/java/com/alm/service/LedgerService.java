package com.alm.service;

import com.alm.dto.GeneralLedgerDTO;
import com.alm.repository.LedgerRepository;
import java.util.List;
import java.util.Map;

/**
 * [Service 계층 - Business Logic]
 * 가계부 도메인의 핵심 비즈니스 로직을 수행하는 클래스.
 * Controller로부터 요청을 받아 Repository를 호출하고, 결과를 가공하여 반환.
 *
 * [계층 구조에서의 역할]
 *   Controller → Service → Repository → DB
 *   - Controller: HTTP 요청/응답 처리만 담당.
 *   - Service: 실제 비즈니스 판단 (잔액 조정, 유효성 검사 등).
 *   - Repository: SQL 실행 결과 반환.
 *
 * [주요 비즈니스 규칙]
 *   1. 가계부 내역 저장 시 → 연동 자산의 잔액을 즉시 반영 (트랜잭션 연동).
 *   2. 가계부 내역 삭제 시 → 저장된 금액을 자산 잔액에 역방향으로 복원.
 *   3. 자산 연동 없이 (asset_id = 0) 기록만 저장하는 것도 허용.
 */
public class LedgerService {

    // [Java 문법 개념] final 필드:
    //   - final이 붙은 참조 변수는 한 번 초기화 후 다른 객체를 재할당(=) 불가.
    //   - 필드가 가리키는 객체의 내부 상태(메서드 호출)는 여전히 변경 가능.
    //   - 의도치 않은 참조 교체를 컴파일 시점에 방지하는 불변성(Immutability) 표현.
    private final LedgerRepository ledgerRepository = new LedgerRepository();

    // ─────────────────────────────────────────────────────────────────
    // 조회
    // ─────────────────────────────────────────────────────────────────

    /**
     * 전체 일반 지출입 내역 목록을 반환.
     *
     * @return GeneralLedgerDTO 리스트 (최신순 정렬은 Repository SQL에서 처리됨)
     */
    public List<GeneralLedgerDTO> getAllGeneralLedger() {
        // Service가 Repository에 위임(delegation)하는 단순 전달 메서드.
        // 별도 비즈니스 로직 없이 Repository 결과를 그대로 반환.
        return ledgerRepository.findAllGeneralLedger();
    }

    /**
     * 이달의 수입/지출/순수지 합계를 반환.
     *
     * @return Map with keys: "totalIncome", "totalExpense", "net"
     */
    public Map<String, Long> getMonthlySummary() {
        // Repository 메서드명이 기획서 기준 getSumByCategory()로 변경됨 → 그 메서드를 호출
        return ledgerRepository.getSumByCategory();
    }

    /**
     * 가계부 내역 추가 폼에서 사용할 카테고리 드롭다운 목록 반환.
     *
     * @return [{"category_id": 1, "category_name": "식비"}, ...] 형태
     */
    public List<Map<String, Object>> getCategories() {
        return ledgerRepository.findAllCategories();
    }

    /**
     * 가계부 연동 가능한 자산(ACC + CSH)의 드롭다운 목록 반환.
     *
     * @return [{"asset_id": 1, "display_name": "카카오뱅크 (4567)", "type_code": "ACC"}, ...] 형태
     */
    public List<Map<String, Object>> getLiquidAssets() {
        return ledgerRepository.findLiquidAssets();
    }

    // ─────────────────────────────────────────────────────────────────
    // 저장 (핵심 비즈니스 로직 포함)
    // ─────────────────────────────────────────────────────────────────

    /**
     * 일반 지출입 내역을 저장하고, 연동 자산의 잔액을 즉시 반영.
     *
     * [처리 순서 - 원자성(Atomicity) 고려]
     *   1. ledger_master INSERT → ledger_id 발급
     *   2. general_ledger INSERT
     *   3. 자산 잔액 조정 (account_table 또는 cash_asset의 balance UPDATE)
     *
     * [주의] 순수 JDBC 환경이므로 @Transactional 어노테이션이 없음.
     *   → 2번 성공 후 3번 실패 시 잔액 불일치 가능성 존재.
     *   → 완전한 원자성은 DataSource 기반 트랜잭션 관리(Spring TX)로 해결 가능. (향후 과제)
     *
     * @param payload 프론트엔드에서 전송한 JSON 데이터를 Map으로 수신
     *                {asset_id, category_id, amount(항상 양수), direction("IN"/"OUT"), transaction_date}
     * @throws Exception 마스터 생성 실패, 저장 실패 시 예외 발생 → Controller에서 400 응답 처리
     */
    /**
     * [기획서 5.3절] 일반 지출입 내역 처리 진입점. 기획서 메서드명: processGeneralLedger().
     * saveGeneralLedger에서 이름을 변경. 'process'는 단순 저장(save)을 넘어
     * 마스터 생성 → 상세 저장 → 잔액 조정의 복합 처리(process)임을 의도적으로 표현.
     */
    public void processGeneralLedger(Map<String, Object> payload) throws Exception {

        // [Java 문법 개념] Map.get(key):
        //   - JSON 요청 body가 Map<String, Object>로 역직렬화됨.
        //   - 값의 실제 타입이 Object이므로, 형변환(casting) 또는 toString() 후 파싱 필요.

        // 1. payload에서 direction 추출 (수입/지출 구분)
        // getOrDefault(key, defaultValue): key가 없거나 null이면 defaultValue를 반환.
        String direction = (String) payload.getOrDefault("direction", "OUT");

        // 2. amount 파싱 (항상 양수로 전송됨, 방향은 direction으로 구분)
        long absAmount = parseLongSafe(payload.get("amount"));

        // 3. 부호 적용: 수입(IN)이면 양수, 지출(OUT)이면 음수로 저장
        // 삼항 연산자(Ternary Operator): 조건 ? 참일때값 : 거짓일때값
        long signedAmount = "IN".equals(direction) ? absAmount : -absAmount;

        // 4. asset_id 파싱 (선택 항목: 0이면 연동 자산 없음)
        long assetId = parseLongSafe(payload.get("asset_id"));

        // 5. ledger_master에 부모 레코드 먼저 생성 (Class Table Inheritance 패턴)
        int ledgerId = ledgerRepository.insertLedgerMaster("GEN");
        if (ledgerId == -1) throw new Exception("가계부 마스터 생성에 실패했습니다.");

        // 6. GeneralLedgerDTO 조립
        GeneralLedgerDTO dto = new GeneralLedgerDTO();
        dto.setLedger_id(ledgerId);                              // 마스터에서 발급받은 ID
        dto.setType_code("GEN");
        dto.setAsset_id(assetId);
        dto.setCategory_id(parseIntSafe(payload.get("category_id"), 1));
        dto.setAmount(signedAmount);                             // 부호 포함 금액
        dto.setTransaction_date((String) payload.getOrDefault("transaction_date", ""));

        // 7. general_ledger에 실제 데이터 삽입
        boolean saved = ledgerRepository.insertGeneralLedger(dto);
        if (!saved) throw new Exception("가계부 내역 저장에 실패했습니다.");

        // 8. 연동 자산이 있으면 잔액 즉시 반영
        // assetId > 0 조건: 자산 연동 없이 기록만 저장하는 경우를 허용
        if (assetId > 0) {
            // signedAmount 그대로 전달:
            //   수입(양수) → 잔액 증가, 지출(음수) → 잔액 감소
            ledgerRepository.adjustAssetBalance(assetId, signedAmount);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // 삭제 (역방향 잔액 복원 포함)
    // ─────────────────────────────────────────────────────────────────

    /**
     * 가계부 내역을 삭제하고, 연동 자산의 잔액을 삭제 전 상태로 복원.
     *
     * [처리 순서]
     *   1. 삭제할 내역의 asset_id와 amount를 먼저 조회 (삭제 후에는 조회 불가)
     *   2. ledger_master 삭제 → ON DELETE CASCADE로 general_ledger도 자동 삭제
     *   3. 자산 잔액을 원래 amount의 반대 방향으로 복원
     *
     * [환불(Refund) 로직]
     *   기존에 지출(-10000)로 등록된 내역을 삭제하면:
     *   → adjustAssetBalance(assetId, -(-10000)) = +10000 → 잔액이 다시 증가함.
     *   기존에 수입(+50000)으로 등록된 내역을 삭제하면:
     *   → adjustAssetBalance(assetId, -(+50000)) = -50000 → 잔액이 줄어듦.
     *
     * @param ledgerId 삭제할 내역의 ledger_id
     * @throws Exception 내역 없음 또는 삭제 실패 시
     */
    public void deleteGeneralLedger(int ledgerId) throws Exception {

        // 1. 삭제 전 데이터 조회 (복원용)
        // [중요] 이 조회는 반드시 삭제 전에 수행해야 함.
        //        삭제 후 조회하면 null이 반환되어 NullPointerException 발생 가능.
        GeneralLedgerDTO existing = ledgerRepository.findGeneralLedgerById(ledgerId);
        if (existing == null) throw new Exception("해당 내역을 찾을 수 없습니다.");

        long assetId      = existing.getAsset_id();
        long savedAmount  = existing.getAmount(); // 원래 저장된 부호 포함 금액

        // 2. 삭제 실행 (CASCADE로 general_ledger도 함께 삭제됨)
        boolean deleted = ledgerRepository.deleteLedgerMaster(ledgerId);
        if (!deleted) throw new Exception("삭제에 실패했습니다.");

        // 3. 잔액 복원: 원래 amount의 부호를 반전(negation)하여 잔액 되돌리기
        //    단항 연산자 '-': 숫자의 부호를 반전. -savedAmount = savedAmount * -1
        if (assetId > 0) {
            ledgerRepository.adjustAssetBalance(assetId, -savedAmount);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // 스케줄 유틸리티
    // ─────────────────────────────────────────────────────────────────

    /**
     * [기획서 5.3절] 주기 정보(p_value, p_unit)를 기준으로 다음 실행일을 계산.
     * 현재는 스텁(stub) 구현 — 고정지출/정기수입/변동지출 스케줄러 기능 구현 시 완성 예정.
     *
     * [Java 문법 개념] 스텁(Stub) 메서드:
     *   - 실제 구현 없이 메서드 시그니처(이름, 파라미터, 반환 타입)만 정의한 상태.
     *   - 기획서에 명시된 인터페이스를 코드 구조에 반영해 두는 목적.
     *   - UnsupportedOperationException: 아직 구현되지 않은 기능을 호출했음을 알리는 표준 예외.
     *     RuntimeException 하위 클래스이므로 호출자가 catch 없이 컴파일 가능.
     *
     * @param baseDate 기준 날짜 (형식: "YYYY-MM-DD")
     * @param pValue   주기 값 (예: 1, 7, 30)
     * @param pUnit    주기 단위 (예: "DAY", "WEEK", "MONTH")
     * @return 다음 실행일 문자열 (형식: "YYYY-MM-DD")
     * @throws UnsupportedOperationException 미구현 상태에서 호출 시
     */
    public String calculateNextDate(String baseDate, int pValue, String pUnit) {
        // TODO: java.time.LocalDate + DateTimeFormatter로 날짜 계산 구현 예정
        // 예시 로직: LocalDate.parse(baseDate).plusDays(pValue) (pUnit=="DAY"일 때)
        throw new UnsupportedOperationException("calculateNextDate() 미구현 - 스케줄러 기능 개발 시 완성 예정");
    }

    // ─────────────────────────────────────────────────────────────────
    // 유틸리티 메서드 (파싱 방어 로직)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Object → long 안전 변환.
     * JSON으로 전달된 숫자가 Integer, Long, String 등 다양한 타입으로 역직렬화될 수 있으므로
     * toString() 후 Long.parseLong()으로 통일하여 파싱. 실패 시 0L 반환.
     *
     * [Java 문법 개념] Method Overloading:
     *   - 같은 이름의 메서드를 매개변수 타입/개수를 달리하여 여러 개 정의하는 것.
     *   - parseIntSafe(obj, defaultVal)와 parseLongSafe(obj)는 이름이 달라 오버로딩은 아님.
     */
    private long parseLongSafe(Object obj) {
        if (obj == null) return 0L; // null 체크: NPE 방지
        String str = obj.toString().trim(); // trim(): 문자열 앞뒤 공백 제거
        if (str.isEmpty() || "null".equals(str)) return 0L;
        try {
            return Long.parseLong(str); // 문자열 → long 변환. 숫자가 아닌 문자 포함 시 NumberFormatException 발생
        } catch (NumberFormatException e) {
            return 0L; // 파싱 실패 시 기본값 반환 (예외를 던지지 않고 흡수)
        }
    }

    private int parseIntSafe(Object obj, int defaultVal) {
        if (obj == null) return defaultVal;
        String str = obj.toString().trim();
        if (str.isEmpty() || "null".equals(str)) return defaultVal;
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }
}
