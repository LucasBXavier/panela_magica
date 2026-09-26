package com.panelamagica.panelamagica.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.panelamagica.panelamagica.exception.BusinessRuleException;

import java.util.Arrays;
import java.util.Locale;

public enum UnidadeMedida {

    GRAMA,
    QUILOGRAMA,
    MILILITRO,
    LITRO,
    UNIDADE,
    XICARA,
    COLHER_SOPA,
    COLHER_CHA,
    A_GOSTO;

    /** Aceita o valor sem diferenciar maiúsculas/minúsculas e ignora espaços nas pontas. */
    @JsonCreator
    public static UnidadeMedida from(String valor) {
        if (valor == null) {
            return null;
        }
        try {
            return valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException(
                    "unidadeMedida inválida: '" + valor + "'. Valores aceitos: " + Arrays.toString(values()));
        }
    }
}
