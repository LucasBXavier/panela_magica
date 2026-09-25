package com.panelamagica.panelamagica.controller;


import com.panelamagica.panelamagica.dto.SuccessResponseDTO;
import com.panelamagica.panelamagica.dto.receitas.ReceitaRequestDTO;
import com.panelamagica.panelamagica.dto.receitas.ReceitaResponseDTO;
import com.panelamagica.panelamagica.dto.receitas.ReceitasUpdateDTO;
import com.panelamagica.panelamagica.service.ReceitaService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @Operation(summary = "Listar minhas receitas",
            description = "Retorna uma lista de todas as receitas cadastradas pelo usuário autenticado.")
    @GetMapping("/minhas-receitas")
    public ResponseEntity<SuccessResponseDTO<List<ReceitaResponseDTO>>> getMinhasReceitas() {
        return ResponseEntity.ok(new SuccessResponseDTO<>(200, "Minhas receitas listadas com sucesso", service.minhasReceitas()));
    }

    @Operation(summary = "Criar receita",
            description = "Cria uma nova receita com os detalhes fornecidos.")
    @PostMapping("/criar")
    public ResponseEntity<SuccessResponseDTO<ReceitaResponseDTO>> criarReceita(@Valid @RequestBody ReceitaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponseDTO<>(201, "Receita criada com sucesso", service.criarReceita(dto)));
    }

    @Operation(summary = "Deletar receita",
            description = "Deleta uma receita específica pelo seu ID.")
    @DeleteMapping("/{id}")
    public ResponseEntity<SuccessResponseDTO<?>> deletarReceita(@PathVariable UUID id) {
        service.deletarReceita(id);
        return ResponseEntity.ok(new SuccessResponseDTO<>(200, "Receita deletada com sucesso"));
    }

    @Operation(summary = "Enviar imagem da receita",
            description = "Envia (ou substitui) a imagem da receita. Formatos aceitos: JPEG, PNG e WEBP, até 5MB. Apenas o dono da receita pode enviar.")
    @PostMapping(value = "/imagem", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SuccessResponseDTO<String>> uploadImagem(@RequestParam("file") MultipartFile file,
                                                                   @RequestParam("receitaId") UUID receitaId) {
        String imageUrl = service.uploadImagemReceita(receitaId, file);
        return ResponseEntity.ok(new SuccessResponseDTO<>(200, "Imagem enviada com sucesso", imageUrl));
    }

    @Operation(summary = "Obter imagem da receita",
            description = "Retorna o binário da imagem da receita (image/jpeg, image/png ou image/webp).")
    @GetMapping("/imagem/{receitaId}")
    public ResponseEntity<byte[]> getImagemReceita(@PathVariable UUID receitaId) {
        return service.getImagemReceita(receitaId);
    }

    @DeleteMapping("/imagem/{receitaId}")
    @Operation(summary = "Deletar imagem da receita",
            description = "Deleta a imagem da receita. Apenas o dono da receita pode deletar.")
    public ResponseEntity<SuccessResponseDTO<?>> deletarImagemReceita(@PathVariable UUID receitaId) {
        service.deletarImagemReceita(receitaId);
        return ResponseEntity.ok(new SuccessResponseDTO<>(200, "Imagem deletada com sucesso"));
    }

    @Operation(summary = "Atualizar receita",
            description = "Atualização parcial: apenas os campos enviados são alterados. Se `ingredientes` for enviado, substitui a lista inteira. Apenas o dono da receita pode atualizar.")
    @PatchMapping("/{id}")
    public ResponseEntity<SuccessResponseDTO<ReceitaResponseDTO>> atualizarReceita(@PathVariable UUID id, @Valid @RequestBody ReceitasUpdateDTO dto) {
        return ResponseEntity.ok(new SuccessResponseDTO<>(200, "Receita atualizada com sucesso", service.atualizarReceita(id, dto)));
    }
}
