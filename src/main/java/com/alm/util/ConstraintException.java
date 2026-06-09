package com.alm.util;

/** 참조 무결성 위반(자산 삭제 불가) 시 던지는 예외. */
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
