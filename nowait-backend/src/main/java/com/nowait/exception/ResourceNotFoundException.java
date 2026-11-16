package com.nowait.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String recurso, String campo, Object valor) {
        super(String.format("%s não encontrado(a) com %s: %s", recurso, campo, valor));
    }

    public ResourceNotFoundException(String mensagem) {
        super(mensagem);
    }
}
