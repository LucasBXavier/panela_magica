package com.panelamagica.panelamagica.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.panelamagica.panelamagica.exception.BusinessRuleException;

import java.util.Arrays;
import java.util.Locale;

public enum Categoria {

    DOCE,
    SALGADO,
    BEBIDA,
    SOBREMESA,
    OUTROS;

    /** Aceita o valor sem diferenciar maiúsculas/minúsculas e ignora espaços nas pontas. */
    @JsonCreator
    public static Categoria from(String valor) {
        if (valor == null) {
            return null;
        }
        try {
            return valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException(
                    "categoria inválida: '" + valor + "'. Valores aceitos: " + Arrays.toString(values()));
        }
    }
}
