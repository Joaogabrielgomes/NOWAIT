package com.nowait.exception;

public class CepNaoEncontradoException extends RuntimeException {

    public CepNaoEncontradoException(String cep) {
        super(String.format("CEP não encontrado: %s", cep));
    }
}
