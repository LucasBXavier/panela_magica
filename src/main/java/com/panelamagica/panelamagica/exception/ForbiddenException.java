package com.panelamagica.panelamagica.exception;

/** Usuário autenticado sem permissão sobre o recurso (ex.: receita de outro usuário); mapeada para HTTP 403. */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
