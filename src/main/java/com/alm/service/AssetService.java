package com.alm.service;

/**
 * [Business Logic Layer] 자산군의 통합 관리 로직을 수행하는 서비스 클래스입니다.
 */
public class AssetService {

    private DeleteValidatorService deleteValidatorService = new DeleteValidatorService();

    /**
     * 기획서 명세: 전체 자산의 합계를 구하는 메소드 [cite: 295]
     * @return 계산된 총 자산 금액
     */
    public long calculateTotalAsset() {
        long totalAmount = 0;
        // (향후 Repository에서 데이터를 받아와 합산하는 세부 로직 구현부)
        return totalAmount;
    }

    /**
     * 기획서 명세: 자산 삭제 시 제약 조건을 확인하기 위해 DeleteValidatorService를 호출하는 메소드 [cite: 296-297]
     * @param assetId 삭제 요청된 자산 식별자
     */
    public void requestDeleteAsset(long assetId) throws Exception {
        // 무결성 검증 서비스 호출
        deleteValidatorService.checkDependency(assetId);

        // (검증 통과 후 DB 삭제를 수행하는 세부 로직 구현부)
    }
}