package com.panelamagica.panelamagica.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioResponseDTO {

    private UUID id;
    private String nome;
    private String email;

    /** ISO-8601 com fuso (ex.: 2026-09-25T21:41:28.096Z). */
    private OffsetDateTime dataCriacao;
}
