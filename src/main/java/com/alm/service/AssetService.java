package com.alm.service;

import com.alm.dto.AssetDTO;
import com.alm.repository.AssetRepository;
import com.alm.repository.InvestRepository;
import com.alm.service.handler.*;
import java.util.List;
import java.util.Map;

/**
 * 자산 도메인 비즈니스 로직.
 *
 * [SOLID 적용 포인트]
 *   OCP : 새 자산 타입 추가 시 HANDLERS Map 에 핸들러만 등록하면 된다. Service 메서드 수정 불필요.
 *   SRP : 저장/수정의 타입별 로직은 각 Handler 클래스가 담당. 이 클래스는 흐름 제어만 한다.
 *   DIP : 기본 생성자(운영)와 의존성 주입 생성자(테스트/교체) 를 분리해 구현체 교체를 허용한다.
 */
public class AssetService {

    private final AssetRepository        assetRepository;
    private final InvestRepository       investRepository;
    private final DeleteValidatorService deleteValidatorService;

    // 전략 패턴: 타입 코드 → 핸들러 매핑. 새 타입은 여기에 한 줄만 추가한다.
    // Map.of() 로 불변 Map 생성 — 런타임 변경 방지.
    private static final Map<String, AssetHandler> HANDLERS = Map.of(
        "ACC", new AccHandler(),
        "REA", new ReaHandler(),
        "PHY", new PhyHandler(),
        "CSH", new CshHandler()
    );

    /** 운영 환경용 기본 생성자. */
    public AssetService() {
        this.assetRepository        = new AssetRepository();
        this.investRepository       = new InvestRepository();
        this.deleteValidatorService = new DeleteValidatorService();
    }

    /** 테스트·교체 환경용 의존성 주입 생성자 (DIP). */
    public AssetService(AssetRepository assetRepository,
                        InvestRepository investRepository,
                        DeleteValidatorService deleteValidatorService) {
        this.assetRepository        = assetRepository;
        this.investRepository       = investRepository;
        this.deleteValidatorService = deleteValidatorService;
    }

    // ── 조회 ─────────────────────────────────────────────────────────

    public List<AssetDTO> getAllAssets() {
        return assetRepository.findAllAssets();
    }

    public AssetDTO getAssetById(long assetId, String typeCode) {
        return assetRepository.findById(assetId, typeCode.toUpperCase());
    }

    /**
     * 전체 자산 합계 계산.
     *
     * [OCP 개선]
     *   이전: instanceof 체인으로 타입마다 다른 getter 를 직접 호출했다.
     *   개선: dto.getAmount() 호출 — 각 DTO 가 자신의 금액을 반환.
     *         새 타입이 생겨도 이 메서드는 수정하지 않아도 된다.
     *
     * [투자 평가액 별도 합산]
     *   증권계좌 balance 는 예수금(현금)만 반영한다.
     *   보유 주식의 평가액(매입단가 × 수량)은 별도로 더해야 전체 투자 자산이 포함된다.
     */
    public long calculateTotalAsset() {
        long total = 0;
        for (AssetDTO dto : assetRepository.findAllAssets()) total += dto.getAmount();
        total += investRepository.getTotalPortfolioValue();
        return total;
    }

    public List<Map<String, Object>> getBanks()        { return assetRepository.findAllBanks(); }
    public List<Map<String, Object>> getAccountTypes() { return assetRepository.findAllAccountTypes(); }

    public Map<String, Object> addBank(String bankName) {
        if (bankName == null || bankName.trim().isEmpty())
            throw new IllegalArgumentException("은행명을 입력하세요.");
        int id = assetRepository.insertBank(bankName.trim());
        if (id < 0) throw new RuntimeException("은행 추가에 실패했습니다.");
        return Map.of("bank_id", id, "bank_name", bankName.trim());
    }

    public Map<String, Object> addAccountType(String typeName) {
        if (typeName == null || typeName.trim().isEmpty())
            throw new IllegalArgumentException("계좌 종류명을 입력하세요.");
        int id = assetRepository.insertAccountType(typeName.trim());
        if (id < 0) throw new RuntimeException("계좌 종류 추가에 실패했습니다.");
        return Map.of("type_id", id, "type_name", typeName.trim());
    }

    // ── 저장 ─────────────────────────────────────────────────────────

    /**
     * 자산 추가.
     *
     * [처리 순서]
     *   1. asset_master INSERT → AUTO_INCREMENT ID 발급 (Class Table Inheritance 상위 레코드)
     *   2. typeCode 로 핸들러 조회 → 해당 자식 테이블 INSERT
     *   3. 자식 저장 실패 시 마스터 롤백 (고아 레코드 방지)
     *
     * [트랜잭션 부재]
     *   순수 JDBC 환경으로 @Transactional 미사용. 2단계 실패 시 명시적 롤백.
     */
    public void saveAssetDetails(String typeCode, Map<String, Object> payload) throws Exception {
        long assetId = assetRepository.insertAssetMaster(typeCode);
        if (assetId == -1) throw new Exception("마스터 생성 실패");

        AssetHandler handler = HANDLERS.get(typeCode.toUpperCase());
        if (handler == null) {
            assetRepository.deleteById(assetId);
            throw new Exception("알 수 없는 자산 타입: " + typeCode);
        }

        if (!handler.save(assetId, payload, assetRepository)) {
            assetRepository.deleteById(assetId);
            throw new Exception("상세 정보 저장에 실패했습니다.");
        }
    }

    // ── 수정 ─────────────────────────────────────────────────────────

    public void updateAssetDetails(long assetId, String typeCode,
                                   Map<String, Object> payload) throws Exception {
        AssetHandler handler = HANDLERS.get(typeCode.toUpperCase());
        if (handler == null) throw new Exception("알 수 없는 자산 타입: " + typeCode);
        if (!handler.update(assetId, payload, assetRepository))
            throw new Exception("수정에 실패했습니다.");
    }

    // ── 삭제 ─────────────────────────────────────────────────────────

    /**
     * 자산 삭제.
     * "삭제" 문자열 이중 확인 → 참조 무결성 검증 → DB 삭제 순으로 처리.
     */
    public void requestDeleteAsset(long assetId, String confirmString) throws Exception {
        if (!deleteValidatorService.verifyConfirmString(confirmString))
            throw new Exception("문구가 일치하지 않습니다.");
        deleteValidatorService.checkDependency(assetId);
        if (!assetRepository.deleteById(assetId)) throw new Exception("DB 삭제 실패");
    }
}
