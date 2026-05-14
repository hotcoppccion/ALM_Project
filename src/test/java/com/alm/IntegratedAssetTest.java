package com.alm;

import com.alm.controller.AssetController;

public class IntegratedAssetTest {
    public static void main(String[] args) {
        AssetController controller = new AssetController();

        System.out.println("=== [Step 1] 계좌 등록 테스트 ===");
        String regResult = controller.addAccountRequest(1, 1, "123-456-789", 10000000, 2.5);
        System.out.println("결과: " + regResult);

        System.out.println("\n=== [Step 2] 이자 계산 테스트 (ID: 1 가정) ===");
        String interestResult = controller.checkInterestRequest(1);
        System.out.println("결과: " + interestResult);
    }
}