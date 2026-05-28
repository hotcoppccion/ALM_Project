package com.alm.util;

/**
 * 참조 무결성 위반 시 던지는 Checked 예외.
 *
 * [Checked 선택 이유]
 *   "참조 중인 자산 삭제 불가"는 예상 가능한 비즈니스 규칙 위반이다.
 *   컴파일러가 catch 를 강제하면 Controller 에서 실수로 처리를 누락하는 것을 방지할 수 있다.
 *   DeleteValidatorService 가 throw 하고, AssetController 가 catch 하여 400 응답에 담는다.
 */
public class ConstraintException extends Exception {

    private final String errorMessage;

    public ConstraintException(String message) {
        super(message);
        this.errorMessage = message;
    }

    /** e.getMessage() 와 동일한 값. Controller 에서는 getMessage() 사용이 더 일반적. */
    public String getErrorMessage() {
        return errorMessage;
    }
}
