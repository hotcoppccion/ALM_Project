package com.alm.service;

import com.alm.repository.AssetRepository;
import com.alm.dto.*;
import java.util.List;
import java.util.Map;

/**
 * [Service 계층 - Business Logic]
 * 자산 도메인의 핵심 비즈니스 로직을 수행하는 클래스.
 * Controller로부터 요청을 받아 필요한 판단(타입 분기, 유효성 검사)을 수행하고,
 * Repository를 통해 DB 작업을 위임한 후 결과를 반환.
 *
 * [계층 역할 구분]
 *   - Controller: HTTP 요청/응답 형식 처리 담당
 *   - Service(이 클래스): 비즈니스 판단 담당 (어떤 DTO를 만들지, 실패 시 롤백 여부 등)
 *   - Repository: SQL 실행 및 결과 반환만 담당
 *
 * [Java 문법 개념] 클래스 구성요소:
 *   - 필드(Field): 클래스의 상태를 저장하는 변수
 *   - 메서드(Method): 클래스의 동작을 정의하는 함수
 *   - 이 클래스는 생성자를 별도 정의하지 않음 → Java 컴파일러가 기본 생성자(no-arg constructor) 자동 생성
 */
public class AssetService {

    // DeleteValidatorService: 자산 삭제 전 참조 무결성 검증 전담 클래스
    // 단일 책임 원칙(SRP): 검증 로직을 AssetService에 직접 넣지 않고 별도 클래스로 분리
    private final DeleteValidatorService deleteValidatorService = new DeleteValidatorService();

    // AssetRepository: DB 접근을 전담하는 DAO 클래스
    // Service가 Repository를 직접 new로 생성: Spring Bean 주입 미사용 (순수 JDBC 설계)
    private final AssetRepository assetRepository = new AssetRepository();

    // ── 조회 ─────────────────────────────────────────────────────────

    /**
     * 4개 자산 타입(ACC, REA, PHY, CSH)을 모두 조회하여 하나의 List로 반환.
     *
     * [Java 문법 개념] List<AssetDTO> 반환 타입:
     *   - AssetDTO는 abstract class. List에는 실제로 AccountDTO, RealEstateDTO 등 구체 클래스 인스턴스가 담김.
     *   - 다형성(Polymorphism): 부모 타입 참조변수(AssetDTO)로 다양한 자식 객체를 동일하게 처리.
     */
    public List<AssetDTO> getAllAssets() {
        // 단순 위임(Delegation): 비즈니스 로직 없이 Repository 결과를 그대로 반환
        return assetRepository.findAllAssets();
    }

    /**
     * 특정 ID와 타입으로 단건 자산을 조회 (수정 모달 pre-fill용).
     *
     * [Java 문법 개념] toUpperCase():
     *   - String 인스턴스 메서드. 원본 String은 변경 없이 대문자 변환된 새 String 반환.
     *   - Java String은 불변(Immutable)이므로 변환 결과를 반드시 새 변수나 반환값으로 사용해야 함.
     */
    public AssetDTO getAssetById(long assetId, String typeCode) {
        return assetRepository.findById(assetId, typeCode.toUpperCase());
    }

    /**
     * 전체 자산 합계 금액 계산.
     * 4개 타입의 금액 필드가 다르므로 instanceof로 타입 판별 후 해당 getter 호출.
     *
     * [Java 문법 개념] instanceof 연산자:
     *   - 객체가 특정 클래스나 인터페이스의 인스턴스인지 런타임에 검사.
     *   - true이면 해당 타입으로 다운캐스팅(Downcasting) 후 해당 타입의 메서드 호출 가능.
     *   - 예: dto instanceof AccountDTO → dto가 AccountDTO 또는 그 하위 클래스인 경우 true.
     *
     * [Java 문법 개념] 다운캐스팅: (AccountDTO) dto
     *   - 부모 타입 참조(AssetDTO)를 자식 타입(AccountDTO)으로 좁히는 형변환.
     *   - instanceof 검사 없이 다운캐스팅하면 ClassCastException 발생 위험.
     *   - instanceof로 먼저 확인 후 캐스팅하는 것이 안전한 패턴.
     *
     * [Java 문법 개념] 향상된 for문 (Enhanced for / for-each):
     *   - for (타입 변수 : 컬렉션) { } 구문.
     *   - Iterator를 내부적으로 사용. 인덱스 불필요 시 일반 for문보다 간결.
     */
    public long calculateTotalAsset() {
        long totalAmount = 0;                          // 합산 누적 변수 초기화
        List<AssetDTO> list = assetRepository.findAllAssets();

        for (AssetDTO dto : list) {                    // 전체 자산을 순회
            if (dto instanceof AccountDTO)
                totalAmount += ((AccountDTO) dto).getBalance();          // ACC: 잔액
            else if (dto instanceof RealEstateDTO)
                totalAmount += ((RealEstateDTO) dto).getPrice();         // REA: 부동산 금액
            else if (dto instanceof PhysicalAssetDTO)
                totalAmount += ((PhysicalAssetDTO) dto).getCurrent_value(); // PHY: 현재 평가액
            else if (dto instanceof CashAssetDTO)
                totalAmount += ((CashAssetDTO) dto).getBalance();        // CSH: 현금 잔액
        }
        return totalAmount;
    }

    /**
     * 은행 목록을 반환 (계좌 등록/수정 폼 드롭다운용).
     * Map<String, Object> 형태: [{"bank_id": 1, "bank_name": "카카오뱅크"}, ...]
     *
     * [Java 문법 개념] List<Map<String, Object>>:
     *   - 고정된 DTO 클래스 없이 동적으로 컬럼명(String)과 값(Object)을 쌍으로 운반.
     *   - Object 타입: bank_id(int)와 bank_name(String)이 혼재하므로 공통 조상 Object 사용.
     *   - Jackson: List<Map>은 JSON 배열([{}, {}])로 직렬화됨.
     */
    public List<Map<String, Object>> getBanks() {
        return assetRepository.findAllBanks();
    }

    /**
     * 계좌 종류 목록을 반환 (계좌 등록/수정 폼 드롭다운용).
     * 예: [{"type_id": 1, "type_name": "입출금통장"}, ...]
     */
    public List<Map<String, Object>> getAccountTypes() {
        return assetRepository.findAllAccountTypes();
    }

    // ── 저장 ─────────────────────────────────────────────────────────

    /**
     * typeCode에 따라 적절한 DTO를 생성하고 Repository에 저장 요청.
     * Master-Detail 2단계 INSERT: asset_master 먼저 → 상세 테이블 후.
     *
     * [Java 문법 개념] throws Exception:
     *   - 메서드 시그니처에 throws 선언 = 이 메서드는 Exception을 던질 수 있음을 명시.
     *   - checked exception이므로 호출자(AssetController)가 try-catch로 반드시 처리해야 함.
     *
     * [Java 문법 개념] String.equals() vs switch-case:
     *   - if-else if 체인 대신 switch(typeCode)도 가능하나 Java 14 이전은 String switch가 제한적.
     *   - "ACC".equals(typeCode): 리터럴 상수를 앞에 놓아 typeCode가 null이어도 NPE 방지.
     *
     * [Java 문법 개념] Map.getOrDefault(key, defaultValue):
     *   - Map에서 key에 해당하는 값을 가져오되, 키가 없거나 값이 null이면 defaultValue 반환.
     *   - payload.get("acc_number")가 null이면 NPE 발생 가능 → getOrDefault로 안전하게 처리.
     */
    public void saveAssetDetails(String typeCode, Map<String, Object> payload) throws Exception {
        // 1단계: asset_master에 부모 레코드 INSERT → AUTO_INCREMENT ID 발급
        long assetId = assetRepository.insertAssetMaster(typeCode);
        if (assetId == -1) throw new Exception("마스터 생성 실패");

        boolean isSaved = false; // 상세 테이블 저장 성공 여부 플래그

        // typeCode별 분기: 각 타입에 맞는 DTO 생성 후 해당 Repository 메서드 호출
        if ("ACC".equals(typeCode)) {
            AccountDTO dto = new AccountDTO();
            dto.setAsset_id(assetId);                                           // 마스터에서 발급받은 ID
            dto.setAcc_number((String) payload.getOrDefault("acc_number", ""));
            dto.setBalance(parseLongSafe(payload.get("balance")));
            dto.setAccount_interest(parseDoubleSafe(payload.get("account_interest")));
            int bankId = parseIntSafe(payload.get("bank_id"), 1);
            int typeId = parseIntSafe(payload.get("type_id"), 1);
            isSaved = assetRepository.insertAccountDetails(dto, bankId, typeId);

        } else if ("REA".equals(typeCode)) {
            RealEstateDTO dto = new RealEstateDTO();
            dto.setAsset_id(assetId);
            dto.setContract_type((String) payload.getOrDefault("contract_type", ""));
            dto.setAddress((String) payload.getOrDefault("address", ""));
            dto.setPrice(parseLongSafe(payload.get("price")));
            isSaved = assetRepository.insertRealEstateDetails(dto);

        } else if ("PHY".equals(typeCode)) {
            PhysicalAssetDTO dto = new PhysicalAssetDTO();
            dto.setAsset_id(assetId);
            dto.setItem_name((String) payload.getOrDefault("item_name", ""));
            dto.setPurchase_price(parseLongSafe(payload.get("purchase_price")));
            dto.setCurrent_value(parseLongSafe(payload.get("current_value")));
            isSaved = assetRepository.insertPhysicalDetails(dto);

        } else if ("CSH".equals(typeCode)) {
            CashAssetDTO dto = new CashAssetDTO();
            dto.setAsset_id(assetId);
            dto.setName((String) payload.getOrDefault("name", ""));
            dto.setBalance(parseLongSafe(payload.get("balance")));
            isSaved = assetRepository.insertCashDetails(dto);
        }

        // 상세 저장 실패 시 마스터 롤백: 고아(orphan) 레코드 방지
        // [주의] 순수 JDBC라 @Transactional 미사용 → 명시적 롤백 처리
        if (!isSaved) {
            assetRepository.deleteById(assetId); // 마스터 삭제 (상세는 아직 없음)
            throw new Exception("상세 정보 저장에 실패했습니다.");
        }
    }

    // ── 수정 ─────────────────────────────────────────────────────────

    /**
     * typeCode에 따라 해당 상세 테이블을 UPDATE.
     *
     * [Java 문법 개념] boolean 플래그 패턴:
     *   - isUpdated = false로 초기화 후, 각 분기에서 Repository 반환값으로 갱신.
     *   - 분기 이후 !isUpdated 체크로 어느 분기에서도 성공하지 못한 경우 예외 발생.
     */
    public void updateAssetDetails(long assetId, String typeCode, Map<String, Object> payload) throws Exception {
        boolean isUpdated = false;

        if ("ACC".equals(typeCode)) {
            AccountDTO dto = new AccountDTO();
            dto.setAsset_id(assetId);
            dto.setAcc_number((String) payload.getOrDefault("acc_number", ""));
            dto.setBalance(parseLongSafe(payload.get("balance")));
            dto.setAccount_interest(parseDoubleSafe(payload.get("account_interest")));
            int bankId = parseIntSafe(payload.get("bank_id"), 1);
            int typeId = parseIntSafe(payload.get("type_id"), 1);
            isUpdated = assetRepository.updateAccountDetails(dto, bankId, typeId);

        } else if ("REA".equals(typeCode)) {
            RealEstateDTO dto = new RealEstateDTO();
            dto.setAsset_id(assetId);
            dto.setContract_type((String) payload.getOrDefault("contract_type", ""));
            dto.setAddress((String) payload.getOrDefault("address", ""));
            dto.setPrice(parseLongSafe(payload.get("price")));
            isUpdated = assetRepository.updateRealEstateDetails(dto);

        } else if ("PHY".equals(typeCode)) {
            PhysicalAssetDTO dto = new PhysicalAssetDTO();
            dto.setAsset_id(assetId);
            dto.setItem_name((String) payload.getOrDefault("item_name", ""));
            dto.setPurchase_price(parseLongSafe(payload.get("purchase_price")));
            dto.setCurrent_value(parseLongSafe(payload.get("current_value")));
            isUpdated = assetRepository.updatePhysicalDetails(dto);

        } else if ("CSH".equals(typeCode)) {
            CashAssetDTO dto = new CashAssetDTO();
            dto.setAsset_id(assetId);
            dto.setName((String) payload.getOrDefault("name", ""));
            dto.setBalance(parseLongSafe(payload.get("balance")));
            isUpdated = assetRepository.updateCashDetails(dto);
        }

        if (!isUpdated) throw new Exception("수정에 실패했습니다.");
    }

    // ── 삭제 ─────────────────────────────────────────────────────────

    /**
     * 확인 문자열 검증 → 참조 무결성 검사 → 자산 삭제 순서로 처리.
     *
     * [Java 문법 개념] 메서드 호출 체인과 예외 전파:
     *   - verifyConfirmString(), checkDependency(), deleteById() 각각이 실패 시 예외를 throw.
     *   - 이 메서드는 catch 없이 throws Exception으로 선언 → 발생한 예외를 Controller까지 전파.
     *   - Controller에서 catch(Exception e)로 최종 처리하여 HTTP 400 응답으로 변환.
     *
     * [설계] "삭제" 문자열 확인:
     *   - 실수로 삭제 버튼 클릭 방지를 위한 이중 확인(Double Confirmation) 패턴.
     *   - 사용자가 "삭제" 문자를 직접 입력해야만 삭제 진행. UX + 보안 목적.
     */
    public void requestDeleteAsset(long assetId, String confirmString) throws Exception {
        // 1. 확인 문자열 검증: "삭제"가 아니면 즉시 실패
        if (!deleteValidatorService.verifyConfirmString(confirmString))
            throw new Exception("문구가 일치하지 않습니다.");

        // 2. 참조 무결성 검증: 가계부 이력 등 참조 데이터 존재 시 ConstraintException 발생
        deleteValidatorService.checkDependency(assetId);

        // 3. DB 삭제: asset_master 행 삭제 (CASCADE로 자식 테이블 자동 삭제)
        if (!assetRepository.deleteById(assetId)) throw new Exception("DB 삭제 실패");
    }

    // ── 유틸리티 (파싱 방어 로직) ────────────────────────────────────
    // [설계 이유] JSON 역직렬화 시 숫자가 Integer, Double, String 등 다양한 타입으로 올 수 있음.
    // .toString() 후 파싱하면 타입에 무관하게 안전하게 처리 가능.

    /**
     * Object → long 안전 변환. null, 빈 문자열, 파싱 불가 시 0L 반환.
     *
     * [Java 문법 개념] try-catch with return:
     *   - try 블록 내에서 return이 실행되면 catch는 건너뜀.
     *   - catch 블록의 return은 try에서 예외 발생 시에만 실행됨.
     *   - Long.parseLong(): 문자열을 long 기본형으로 변환. 숫자가 아닌 문자 포함 시 NumberFormatException.
     */
    private long parseLongSafe(Object obj) {
        if (obj == null) return 0L;
        String str = obj.toString().trim(); // trim(): 앞뒤 공백 제거 (사용자 입력 오류 방어)
        if (str.isEmpty() || "null".equals(str)) return 0L;
        try { return Long.parseLong(str); } catch (Exception e) { return 0L; }
    }

    /**
     * Object → int 안전 변환. null 또는 파싱 불가 시 defaultVal 반환.
     *
     * [Java 문법 개념] 메서드 오버로딩(Overloading):
     *   - parseLongSafe(Object)와 parseIntSafe(Object, int)는 이름이 달라 오버로딩이 아님.
     *   - 같은 이름으로 파라미터만 다르게 정의하면 오버로딩. 여기선 의도적으로 이름 구분.
     */
    private int parseIntSafe(Object obj, int defaultVal) {
        if (obj == null) return defaultVal;
        String str = obj.toString().trim();
        if (str.isEmpty() || "null".equals(str)) return defaultVal;
        try { return Integer.parseInt(str); } catch (Exception e) { return defaultVal; }
    }

    /**
     * Object → double 안전 변환. 이자율 파싱에 사용.
     * Double.parseDouble(): 문자열을 double 기본형으로 변환. 예: "3.5" → 3.5
     */
    private double parseDoubleSafe(Object obj) {
        if (obj == null) return 0.0;
        String str = obj.toString().trim();
        if (str.isEmpty() || "null".equals(str)) return 0.0;
        try { return Double.parseDouble(str); } catch (Exception e) { return 0.0; }
    }
}
