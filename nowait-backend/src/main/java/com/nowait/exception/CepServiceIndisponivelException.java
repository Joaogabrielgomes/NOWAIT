package com.nowait.exception;

public class CepServiceIndisponivelException extends RuntimeException {

    public CepServiceIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
