package com.alm.service;

import com.alm.dto.AccountDTO;
import com.alm.dto.AssetDTO;
import com.alm.repository.AssetRepository;
import com.alm.repository.InvestRepository;
import com.alm.service.handler.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 자산 도메인 서비스. */
public class AssetService {

    private final AssetRepository        assetRepository;
    private final InvestRepository       investRepository;
    private final DeleteValidatorService deleteValidatorService;
    private final AccountService         accountService;
    private final LedgerService          ledgerService;

    // 타입 코드 → 핸들러 매핑. 새 자산 타입 추가 시 여기에만 등록하면 된다.
    private static final Map<String, AssetHandler> HANDLERS = Map.of(
        "ACC", new AccHandler(),
        "REA", new ReaHandler(),
        "PHY", new PhyHandler(),
        "CSH", new CshHandler()
    );

    /** 기본 생성자. */
    public AssetService() {
        this.assetRepository        = new AssetRepository();
        this.investRepository       = new InvestRepository();
        this.deleteValidatorService = new DeleteValidatorService();
        this.accountService         = new AccountService();
        this.ledgerService          = new LedgerService();
    }

    /** 테스트용 의존성 주입 생성자. */
    public AssetService(AssetRepository assetRepository,
                        InvestRepository investRepository,
                        DeleteValidatorService deleteValidatorService) {
        this.assetRepository        = assetRepository;
        this.investRepository       = investRepository;
        this.deleteValidatorService = deleteValidatorService;
        this.accountService         = new AccountService();
        this.ledgerService          = new LedgerService();
    }

    // ── 조회 ─────────────────────────────────────────────────────────

    public List<AssetDTO> getAllAssets() {
        return assetRepository.findAllAssets();
    }

    public AssetDTO getAssetById(long assetId, String typeCode) {
        return assetRepository.findById(assetId, typeCode.toUpperCase());
    }

    /**
     * 전체 자산 합계.
     * 증권계좌 balance 는 예수금만이므로 포트폴리오 매입 평가액을 별도로 합산한다.
     */
    public long calculateTotalAsset() {
        long total = 0;
        for (AssetDTO dto : assetRepository.findAllAssets()) total += dto.getAmount();
        total += investRepository.getTotalPortfolioValue();
        return total;
    }

    /**
     * 예상 연간 이자 수익금 반환.
     * ACC 타입 자산만 유효하며, 다른 타입이거나 자산이 없으면 0.0 반환.
     */
    public double getAccountInterest(long assetId) {
        AssetDTO dto = assetRepository.findById(assetId, "ACC");
        if (!(dto instanceof AccountDTO)) return 0.0;
        return accountService.calculateInterest((AccountDTO) dto);
    }

    /**
     * 이자 수동 지급.
     * 연간 이자액을 계산 후 계좌 잔액에 반영하고 가계부에 수입으로 기록한다.
     * 기준일은 호출 당일 (사용자가 연 1회 버튼으로 트리거).
     *
     * @throws Exception ACC 타입이 아닌 경우, 이자액이 0 이하인 경우
     */
    public void applyAccountInterest(long assetId) throws Exception {
        AssetDTO dto = assetRepository.findById(assetId, "ACC");
        if (!(dto instanceof AccountDTO))
            throw new Exception("금융계좌(ACC) 타입만 이자 적용이 가능합니다.");

        long interest = Math.round(accountService.calculateInterest((AccountDTO) dto));
        if (interest <= 0)
            throw new Exception("적용할 이자가 없습니다. 이자율 또는 잔액을 확인하세요.");

        Map<String, Object> payload = new HashMap<>();
        payload.put("direction",        "IN");
        payload.put("amount",           interest);
        payload.put("asset_id",         assetId);
        payload.put("category_id",      1);                              // 기본 수입 카테고리
        payload.put("transaction_date", LocalDate.now().toString());

        ledgerService.processGeneralLedger(payload);
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
     * 자산 등록. asset_master INSERT 후 타입별 자식 테이블 INSERT.
     * 자식 저장 실패 시 마스터 행을 삭제하여 고아 레코드를 방지한다.
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

    /** "삭제" 문자열 확인 → 타 도메인 참조 검증 → DB 삭제. */
    public void requestDeleteAsset(long assetId, String confirmString) throws Exception {
        if (!deleteValidatorService.verifyConfirmString(confirmString))
            throw new Exception("문구가 일치하지 않습니다.");
        deleteValidatorService.checkDependency(assetId);
        if (!assetRepository.deleteById(assetId)) throw new Exception("DB 삭제 실패");
    }
}
