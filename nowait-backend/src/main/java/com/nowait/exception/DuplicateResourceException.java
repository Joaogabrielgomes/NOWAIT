package com.nowait.exception;

public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String recurso, String campo, Object valor) {
        super(String.format("%s já existe com %s: %s", recurso, campo, valor));
    }
}
