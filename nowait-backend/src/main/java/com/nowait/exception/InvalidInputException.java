package com.nowait.exception;

public class InvalidInputException extends RuntimeException {

    public InvalidInputException(String mensagem) {
        super(mensagem);
    }
}
