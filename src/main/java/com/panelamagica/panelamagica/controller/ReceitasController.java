package com.panelamagica.panelamagica.controller;


import com.panelamagica.panelamagica.dto.SuccessResponseDTO;
import com.panelamagica.panelamagica.dto.receitas.ReceitaRequestDTO;
import com.panelamagica.panelamagica.dto.receitas.ReceitaResponseDTO;
import com.panelamagica.panelamagica.dto.receitas.ReceitasUpdateDTO;
import com.panelamagica.panelamagica.service.ReceitaService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/receitas")
@RequiredArgsConstructor
public class ReceitasController {

    private final ReceitaService service;

    @Operation(summary = "Listar receitas",
            description = "Retorna uma lista de todas as receitas cadastradas.")
    @GetMapping
    public ResponseEntity<SuccessResponseDTO<List<ReceitaResponseDTO>>> getReceitas() {
        return ResponseEntity.ok(new SuccessResponseDTO<>(200, "Receitas listadas com sucesso", service.getReceitas()));
    }

    @Operation(summary = "Obter receita por ID",
            description = "Retorna os detalhes de uma receita específica pelo seu ID.")
    @GetMapping("/{id}")
    public ResponseEntity<SuccessResponseDTO<ReceitaResponseDTO>> getReceitaById(@PathVariable UUID id) {
        return ResponseEntity.ok(new SuccessResponseDTO<>(200, "Receita encontrada", service.getReceitasById(id)));
    }

    @Operation(summary = "Criar receita",
            description = "Cria uma nova receita com os detalhes fornecidos. Retorna 201 com o header `Location` da receita criada.")
    @PostMapping
    public ResponseEntity<SuccessResponseDTO<ReceitaResponseDTO>> criarReceita(@Valid @RequestBody ReceitaRequestDTO dto) {
        ReceitaResponseDTO criada = service.criarReceita(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criada.getId())
                .toUri();
        return ResponseEntity.created(location)
                .body(new SuccessResponseDTO<>(201, "Receita criada com sucesso", criada));
    }

    @Operation(summary = "Atualizar receita",
            description = "Atualização parcial: apenas os campos enviados são alterados. Se `ingredientes` for enviado, substitui a lista inteira. Apenas o dono da receita pode atualizar.")
    @PatchMapping("/{id}")
    public ResponseEntity<SuccessResponseDTO<ReceitaResponseDTO>> atualizarReceita(@PathVariable UUID id, @Valid @RequestBody ReceitasUpdateDTO dto) {
        return ResponseEntity.ok(new SuccessResponseDTO<>(200, "Receita atualizada com sucesso", service.atualizarReceita(id, dto)));
    }

    @Operation(summary = "Deletar receita",
            description = "Deleta uma receita específica pelo seu ID. Retorna 204 sem corpo.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarReceita(@PathVariable UUID id) {
        service.deletarReceita(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Enviar imagem da receita",
            description = "Envia (ou substitui) a imagem da receita. Formatos aceitos: JPEG, PNG e WEBP, até 5MB. Apenas o dono da receita pode enviar.")
    @PutMapping(value = "/{id}/imagem", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SuccessResponseDTO<String>> uploadImagem(@PathVariable UUID id,
                                                                   @RequestParam("file") MultipartFile file) {
        String imageUrl = service.uploadImagemReceita(id, file);
        return ResponseEntity.ok(new SuccessResponseDTO<>(200, "Imagem enviada com sucesso", imageUrl));
    }

    @Operation(summary = "Obter imagem da receita",
            description = "Retorna o binário da imagem da receita (image/jpeg, image/png ou image/webp).")
    @GetMapping("/{id}/imagem")
    public ResponseEntity<byte[]> getImagemReceita(@PathVariable UUID id) {
        return service.getImagemReceita(id);
    }

    @Operation(summary = "Deletar imagem da receita",
            description = "Deleta a imagem da receita. Apenas o dono da receita pode deletar. Retorna 204 sem corpo.")
    @DeleteMapping("/{id}/imagem")
    public ResponseEntity<Void> deletarImagemReceita(@PathVariable UUID id) {
        service.deletarImagemReceita(id);
        return ResponseEntity.noContent().build();
    }
}
