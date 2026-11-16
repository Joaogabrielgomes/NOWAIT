package com.nowait.exception;

public class UnauthorizedOperationException extends RuntimeException {

    public UnauthorizedOperationException(String mensagem) {
        super(mensagem);
    }
}
