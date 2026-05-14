package com.alm.util;

/**
 * [Exception Layer] 시스템 내 참조 무결성 및 제약 조건 위반 시 발생하는 사용자 정의 예외 클래스입니다.
 * 데이터 삭제 등 민감한 작업 수행 중 발생할 수 있는 오류의 구체적인 사유를 전달합니다.
 */
public class ConstraintException extends Exception {

    // 예외 발생 시 화면에 출력될 구체적인 에러 메시지
    private final String errorMessage;

    public ConstraintException(String message) {
        super(message);
        this.errorMessage = message;
    }

    /**
     * 발생한 예외의 상세 에러 메시지를 반환합니다.
     * @return 구체적인 제약 조건 위반 사유
     */
    public String getErrorMessage() {
        return errorMessage;
    }
}