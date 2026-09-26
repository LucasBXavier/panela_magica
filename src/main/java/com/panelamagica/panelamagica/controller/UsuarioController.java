package com.panelamagica.panelamagica.controller;

import com.panelamagica.panelamagica.dto.SuccessResponseDTO;
import com.panelamagica.panelamagica.dto.receitas.ReceitaResponseDTO;
import com.panelamagica.panelamagica.dto.user.UsuarioRequestDTO;
import com.panelamagica.panelamagica.service.ReceitaService;
import com.panelamagica.panelamagica.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final ReceitaService receitaService;

    @Operation(summary = "Cadastrar usuário",
            description = "Cadastra um novo usuário no sistema.")
    @PostMapping
    public ResponseEntity<SuccessResponseDTO<?>> cadastrar(@Valid @RequestBody UsuarioRequestDTO dto) {
        usuarioService.cadastrar(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponseDTO<>(201, "Usuário cadastrado com sucesso!"));
    }

    @Operation(summary = "Listar minhas receitas",
            description = "Retorna todas as receitas cadastradas pelo usuário autenticado.")
    @GetMapping("/me/receitas")
    public ResponseEntity<SuccessResponseDTO<List<ReceitaResponseDTO>>> getMinhasReceitas() {
        return ResponseEntity.ok(new SuccessResponseDTO<>(200, "Minhas receitas listadas com sucesso", receitaService.minhasReceitas()));
    }
}
