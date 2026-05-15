package com.alm.controller;

import com.alm.service.AssetService;
import com.alm.dto.AssetDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/**
 * [Controller 계층 - REST API]
 * 자산 도메인의 HTTP 요청을 수신하고 적절한 Service 메서드로 위임한 뒤, 응답을 반환하는 최전방 클래스.
 *
 * [Java 문법 개념] @RestController:
 *   - @Controller + @ResponseBody를 합친 합성 어노테이션(Composed Annotation).
 *   - @Controller: Spring이 이 클래스를 웹 요청 처리 Bean으로 인식하게 함.
 *   - @ResponseBody: 메서드 반환값을 HTTP 응답 body에 JSON으로 직렬화.
 *     Jackson 라이브러리가 List<AssetDTO> → JSON 배열, Map → JSON 객체로 변환.
 *
 * [Java 문법 개념] @RequestMapping("/api/asset"):
 *   - 클래스 레벨 어노테이션: 이 클래스 내 모든 메서드의 URL에 "/api/asset" 접두사를 공통 적용.
 *   - 각 메서드의 @GetMapping, @PostMapping 등 경로는 이 접두사 뒤에 추가됨.
 *   - 예: @GetMapping("/list") → 실제 URL = /api/asset/list
 *
 * [계층 책임]
 *   - 비즈니스 로직 수행 금지. 오직 요청 수신 → Service 호출 → 응답 반환만 담당.
 *   - 예외 발생 시 catch하여 400 Bad Request로 변환하는 것도 Controller 역할.
 */
@RestController
@RequestMapping("/api/asset")
public class AssetController {

    // [Java 문법 개념] 필드 선언과 동시에 초기화 (Eager Initialization):
    //   - 생성자 없이 new AssetService()를 필드 선언부에서 즉시 실행.
    //   - final: 한 번 참조 대입 후 재할당 불가. 실수로 assetService = new AssetService()를
    //     메서드 내에서 재실행하는 버그를 컴파일 시점에 차단.
    //   - Spring @Autowired 미사용: 순수 JDBC 설계로 DI 컨테이너 의존성 최소화.
    private final AssetService assetService = new AssetService();

    // ── 조회 엔드포인트 ──────────────────────────────────────────────

    /**
     * 전체 자산 목록을 4개 타입(ACC, REA, PHY, CSH) 합산하여 반환.
     * GET /api/asset/list
     *
     * [Java 문법 개념] ResponseEntity<List<AssetDTO>>:
     *   - ResponseEntity: HTTP 상태 코드, 헤더, body를 포함하는 Spring 응답 래퍼 클래스.
     *   - <List<AssetDTO>>: 제네릭(Generic) 타입 파라미터. body에 담을 데이터 타입을 컴파일 타임에 지정.
     *   - ResponseEntity.ok(body): 상태 200 OK + body 조합을 편리하게 생성하는 정적 팩토리 메서드.
     *   - AssetDTO는 abstract class이므로 List엔 실제로 AccountDTO, RealEstateDTO 등이 담김.
     *     이것이 다형성(Polymorphism): 부모 타입 참조변수로 자식 객체를 저장.
     */
    @GetMapping("/list")
    public ResponseEntity<List<AssetDTO>> getAllAssets() {
        // assetService.getAllAssets()는 4개 타입을 각각 조회 후 하나의 List로 합쳐 반환
        return ResponseEntity.ok(assetService.getAllAssets());
    }

    /**
     * 전체 자산 합계 금액(totalAmount)을 반환.
     * GET /api/asset/total
     * 응답 예: {"totalAmount": 15000000}
     *
     * [Java 문법 개념] Map.of("key", value):
     *   - Java 9에서 추가된 불변 Map 생성 팩토리 메서드.
     *   - new HashMap<>()보다 간결하며, 생성 후 put() 호출 불가 (불변).
     *   - Jackson이 {"totalAmount": 15000000} JSON 객체로 직렬화함.
     */
    @GetMapping("/total")
    public ResponseEntity<?> getTotalAsset() {
        // <?> 와일드카드: 반환 타입이 상황에 따라 달라질 때 사용. Map 또는 에러 body 모두 가능.
        return ResponseEntity.ok(Map.of("totalAmount", assetService.calculateTotalAsset()));
    }

    /**
     * 수정 모달 pre-fill용 단건 자산 조회.
     * GET /api/asset/{assetId}?typeCode=ACC
     *
     * [Java 문법 개념] @PathVariable:
     *   - URL 경로의 {} 템플릿 변수를 메서드 파라미터로 바인딩.
     *   - GET /api/asset/5?typeCode=ACC → assetId=5, typeCode="ACC"
     *
     * [Java 문법 개념] @RequestParam:
     *   - URL 쿼리 파라미터(?key=value)를 메서드 파라미터로 바인딩.
     *   - required=true가 기본값이므로 typeCode가 없으면 400 자동 반환.
     */
    @GetMapping("/{assetId}")
    public ResponseEntity<?> getAssetById(@PathVariable long assetId,
                                          @RequestParam String typeCode) {
        AssetDTO dto = assetService.getAssetById(assetId, typeCode);
        // dto == null: DB에 해당 ID가 없는 경우. 404 Not Found로 응답.
        if (dto == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(dto); // Jackson이 실제 타입(AccountDTO 등)의 필드를 JSON으로 직렬화
    }

    /**
     * 계좌 등록 폼에서 사용할 은행 목록 드롭다운 반환.
     * GET /api/asset/banks
     * 응답 예: [{"bank_id": 1, "bank_name": "카카오뱅크"}, ...]
     */
    @GetMapping("/banks")
    public ResponseEntity<?> getBanks() {
        return ResponseEntity.ok(assetService.getBanks());
    }

    /**
     * 계좌 등록 폼에서 사용할 계좌 종류 목록 드롭다운 반환.
     * GET /api/asset/account-types
     * 응답 예: [{"type_id": 1, "type_name": "입출금통장"}, ...]
     */
    @GetMapping("/account-types")
    public ResponseEntity<?> getAccountTypes() {
        return ResponseEntity.ok(assetService.getAccountTypes());
    }

    // ── 저장 엔드포인트 ──────────────────────────────────────────────

    /**
     * [기획서 5.2절] 자산 목록 조회 (getAllAssets 별칭).
     * GET /api/asset
     * getAllAssets()와 동일한 결과를 반환. 기획서 메서드명 getAssetList()에 대응.
     *
     * [Java 문법 개념] 메서드 오버로딩(Overloading)과의 차이:
     *   - 이 메서드는 오버로딩이 아닌, URL 매핑만 다른 별도의 위임(delegation) 메서드.
     *   - @GetMapping(""): 클래스 레벨 @RequestMapping("/api/asset") + ""  → GET /api/asset
     *   - 내부에서 assetService.getAllAssets()를 그대로 호출하여 코드 중복 없이 재사용.
     */
    @GetMapping("")
    public ResponseEntity<List<AssetDTO>> getAssetList() {
        // getAllAssets()에 위임: 기획서의 getAssetList() 메서드명을 코드에서도 표현
        return ResponseEntity.ok(assetService.getAllAssets());
    }

    /**
     * 새 자산 등록. typeCode에 따라 해당 상세 테이블에 저장.
     * POST /api/asset/{typeCode}  (예: POST /api/asset/ACC)
     *
     * [Java 문법 개념] @PostMapping + @RequestBody:
     *   - @PostMapping: HTTP POST 메서드 요청을 이 메서드에 매핑.
     *   - @RequestBody: HTTP 요청 body의 JSON → Java Map<String, Object>로 역직렬화.
     *     Jackson이 자동 처리. 예: {"acc_number": "123", "balance": 50000}
     *
     * [Java 문법 개념] String.toUpperCase():
     *   - 문자열의 모든 알파벳을 대문자로 변환. 예: "acc" → "ACC"
     *   - 프론트엔드에서 소문자로 전송해도 서비스 레이어에서 대문자로 통일하여 처리.
     */
    @PostMapping("/{typeCode}")
    public ResponseEntity<?> registerAsset(@PathVariable String typeCode,
                                           @RequestBody Map<String, Object> payload) {
        try {
            assetService.saveAssetDetails(typeCode.toUpperCase(), payload);
            return ResponseEntity.ok(Map.of("message", "자산이 성공적으로 등록되었습니다."));
        } catch (Exception e) {
            // catch (Exception e): Service에서 throw한 모든 예외를 포착.
            // ResponseEntity.badRequest(): 상태 코드 400 Bad Request 반환.
            // e.getMessage(): Exception 생성 시 전달한 메시지 문자열 추출.
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ── 수정 엔드포인트 ──────────────────────────────────────────────

    /**
     * 기존 자산 수정. payload의 type_code로 타입 판별 후 해당 테이블 UPDATE.
     * PUT /api/asset/{assetId}
     *
     * [Java 문법 개념] @PutMapping:
     *   - HTTP PUT 메서드: 리소스 전체를 교체(전체 업데이트). 멱등성 있음.
     *   - PATCH(부분 수정)와 구별. 본 시스템은 PUT으로 전체 필드를 받아 전체 교체.
     *
     * [Java 문법 개념] (String) payload.get("type_code"):
     *   - Map의 값 타입이 Object이므로 String으로 명시적 다운캐스팅(Downcasting) 필요.
     *   - 실제 값이 String이 아니면 ClassCastException 발생. 프론트에서 항상 String으로 전송 보장.
     */
    @PutMapping("/{assetId}")
    public ResponseEntity<?> updateAsset(@PathVariable long assetId,
                                         @RequestBody Map<String, Object> payload) {
        try {
            String typeCode = (String) payload.get("type_code");
            // null 체크: typeCode 없이 PUT 요청이 오면 즉시 400 반환
            if (typeCode == null) return ResponseEntity.badRequest().body(Map.of("message", "type_code 누락"));
            assetService.updateAssetDetails(assetId, typeCode.toUpperCase(), payload);
            return ResponseEntity.ok(Map.of("message", "자산이 수정되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ── 삭제 엔드포인트 ──────────────────────────────────────────────

    /**
     * 자산 삭제. "삭제" 확인 문자열 검증 + 가계부 참조 무결성 검사 후 삭제.
     * DELETE /api/asset/{assetId}?confirmString=삭제
     *
     * [Java 문법 개념] @DeleteMapping:
     *   - HTTP DELETE 메서드: 리소스 삭제. 멱등성 있음 (반복 요청 시 결과 동일).
     *   - @RequestParam String confirmString: URL 쿼리 파라미터로 전달된 확인 문자열.
     *     예: DELETE /api/asset/5?confirmString=삭제
     *
     * [DB 설계 연계] ON DELETE CASCADE:
     *   - asset_master 삭제 시 account_table, real_estate 등 자식 테이블의 연결 행이 자동 삭제.
     *   - DB 수준에서 처리되므로 Java 코드에서 자식 테이블을 별도 삭제할 필요 없음.
     */
    @DeleteMapping("/{assetId}")
    public ResponseEntity<?> deleteAsset(@PathVariable long assetId,
                                         @RequestParam String confirmString) {
        try {
            assetService.requestDeleteAsset(assetId, confirmString);
            return ResponseEntity.ok(Map.of("message", "자산이 삭제되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
