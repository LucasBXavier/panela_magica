package com.panelamagica.panelamagica.exception;

/** Falta de autenticação válida (ex.: refresh token inválido); mapeada para HTTP 401. */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
