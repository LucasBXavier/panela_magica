package com.panelamagica.panelamagica.controller;

import com.panelamagica.panelamagica.dto.SuccessResponseDTO;
import com.panelamagica.panelamagica.dto.login.LoginRequestDTO;
import com.panelamagica.panelamagica.dto.login.LoginResponseDTO;
import com.panelamagica.panelamagica.dto.user.UsuarioRequestDTO;
import com.panelamagica.panelamagica.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @Operation(summary = "Cadastrar usuário",
            description = "Cadastra um novo usuário no sistema.")
    @PostMapping("/cadastrar")
    public ResponseEntity<SuccessResponseDTO<?>> cadastrar(@Valid @RequestBody UsuarioRequestDTO dto) {
        usuarioService.cadastrar(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponseDTO<>(201, "Usuário cadastrado com sucesso!"));
    }

    @Operation(summary = "Login de usuário",
            description = "Realiza o login de um usuário existente no sistema.")
    @PostMapping("/login")
    public ResponseEntity<SuccessResponseDTO<LoginResponseDTO>> login(@Valid @RequestBody LoginRequestDTO dto) {
        LoginResponseDTO response = usuarioService.login(dto);
        return ResponseEntity.ok(new SuccessResponseDTO<>(200, "Login realizado com sucesso!", response));
    }
}
