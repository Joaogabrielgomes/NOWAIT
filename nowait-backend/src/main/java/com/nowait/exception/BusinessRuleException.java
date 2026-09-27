package com.nowait.exception;

public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String mensagem) {
        super(mensagem);
    }
}
