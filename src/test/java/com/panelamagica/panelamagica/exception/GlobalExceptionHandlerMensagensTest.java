package com.panelamagica.panelamagica.exception;

import com.panelamagica.panelamagica.dto.ErrorResponseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerMensagensTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private String mensagem(HttpStatus status) {
        ResponseEntity<ErrorResponseDTO> r = handler.handleUnexpected(new ResponseStatusException(status));
        assertThat(r.getStatusCode()).isEqualTo(status);
        return r.getBody().getMessage();
    }

    @Test
    void mensagensPadraoPorStatus() {
        assertThat(mensagem(HttpStatus.NOT_FOUND)).isEqualTo("Recurso não encontrado");
        assertThat(mensagem(HttpStatus.METHOD_NOT_ALLOWED)).isEqualTo("Método não permitido");
        assertThat(mensagem(HttpStatus.NOT_ACCEPTABLE)).isEqualTo("Formato de resposta não aceito");
        assertThat(mensagem(HttpStatus.UNSUPPORTED_MEDIA_TYPE)).isEqualTo("Tipo de conteúdo não suportado");
        assertThat(mensagem(HttpStatus.CONFLICT)).isEqualTo("Requisição inválida");
        assertThat(mensagem(HttpStatus.BAD_GATEWAY)).isEqualTo("Erro interno do servidor");
    }

    @Test
    void excecaoComumVira500() {
        var r = handler.handleUnexpected(new RuntimeException("x"));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
